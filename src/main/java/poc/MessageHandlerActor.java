package poc;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.io.Tcp;
import akka.io.TcpMessage;
import akka.util.ByteString;
import scala.Tuple2;

import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * Created by mwisniewski.
 */
public class MessageHandlerActor extends AbstractActor {

    final LoggingAdapter log = Logging.getLogger(getContext().getSystem(), getSelf());
    
    private ByteString received = ByteString.empty();
    private ByteString bsHeader = ByteString.empty();
    private ActorRef tcpSender;

    public static Props props() {
        return Props.create(MessageHandlerActor.class);
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(Tcp.Received.class, this::onReceiveData)
                .build();
    }

    private void onReceiveData(Tcp.Received msg) throws InterruptedException {

        try {
            tcpSender = getSender();

            received = received.concat(msg.data());

            int endOfHeaderIdx = received.indexOfSlice(AppConsts.EOH);
            if (endOfHeaderIdx == -1) {
                log.info("End of header not yet found - continue processing header");
                bsHeader = bsHeader.concat(received);
            } else {
                log.info("Found end of header - creating header object");
                Tuple2<ByteString, ByteString> split = received.splitAt(endOfHeaderIdx);
                bsHeader = bsHeader.concat(split._1());
                String rawHeader = bsHeader.utf8String();
                
                if (rawHeader.equals("PING")) {
                    int sleep = new Random().nextInt(10);
                    log.info("Sleep " + sleep + " secs");
                    TimeUnit.SECONDS.sleep(sleep);
                    infoAndClose("Server works!");
                    return;
                }
            }
        } catch (Exception ex) {
            notifyAndClose(ex.getMessage());
            throw ex;
        }
    }
    
    private void infoAndClose(String message) {
        tcpSender.tell(TcpMessage.write(ByteString.fromString(message)), ActorRef.noSender());
        tcpSender.tell(TcpMessage.close(), ActorRef.noSender());
    }
    
    private void notifyAndClose(String message) {
        tcpSender.tell(TcpMessage.write(ByteString.fromString(message)), ActorRef.noSender());
        tcpSender.tell(TcpMessage.close(), ActorRef.noSender());
    }

//    @Override
//    public SupervisorStrategy supervisorStrategy() {
//        return new OneForOneStrategy(10, Duration.create("1 minute"), DeciderBuilder
//                .match(NullPointerException.class, e -> SupervisorStrategy.restart())
//                .build());
//    }
}

