package poc;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.io.Tcp;
import akka.io.Tcp.Event;
import akka.io.TcpMessage;
import akka.util.ByteString;
import scala.Tuple2;

import java.net.InetSocketAddress;
import java.util.Random;
import java.util.concurrent.TimeUnit;

public class MessageHandlerActor extends AbstractActor {

    final LoggingAdapter log = Logging.getLogger(getContext().getSystem(), getSelf());
    
    private ByteString received = ByteString.empty();
    private ByteString bsHeader = ByteString.empty();
    private ActorRef connection;
    private InetSocketAddress remote;

    private final Event ACK = new Event() {};

    public MessageHandlerActor(ActorRef connection, InetSocketAddress remote) {
        this.connection = connection;
        this.remote = remote;

        getContext().watch(connection);
    }
    
    public static Props props(ActorRef connection, InetSocketAddress remote) {
        return Props.create(MessageHandlerActor.class, connection, remote);
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(Tcp.Received.class, this::onReceiveData)
                .match(Tcp.ConnectionClosed.class, msg -> {
                    getContext().stop(getSelf());
                })
                .build();
    }

    private void onReceiveData(Tcp.Received msg) throws InterruptedException {
        
        try {
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
                
                if (rawHeader.contains("PING")) {
                    int sleep = new Random().nextInt(5);
                    TimeUnit.SECONDS.sleep(sleep);
                    notifyAndClose("Server works!");
                    return;
                }
            }
        } catch (Exception ex) {
            notifyAndClose(ex.getMessage());
            throw ex;
        }
    }
    
    private void notifyAndClose(String message) {
        connection.tell(TcpMessage.write(ByteString.fromString(message), ACK), getSelf());
        getContext().become(acknowledgement(), false);
    }
    
    private final Receive acknowledgement() {
        return receiveBuilder()
                .match(Event.class, msg -> msg == ACK, msg -> {
                    log.info("Received ACK");
                    getContext().unbecome();
                    connection.tell(TcpMessage.close(), getSelf());
                })
                .build();
    }

}

