package poc;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.io.Tcp;
import akka.io.Tcp.Bound;
import akka.io.Tcp.CommandFailed;
import akka.io.Tcp.Connected;
import akka.io.TcpMessage;

import java.net.InetSocketAddress;

import static akka.actor.Props.create;

/**
 * Created by mwisniewski.
 */
public class ConnectionActor extends AbstractActor {

    final LoggingAdapter log = Logging.getLogger(getContext().getSystem(), getSelf());

    ActorRef manager;

    public ConnectionActor(ActorRef manager) {
        this.manager = manager;
    }

    public static Props props(ActorRef manager) {
        return create(ConnectionActor.class, manager);
    }

    @Override
    public void preStart() throws Exception {
        final ActorRef tcp = Tcp.get(getContext().system()).manager();
        tcp.tell(TcpMessage.bind(getSelf(), new InetSocketAddress("localhost", 9099), 100), getSelf());
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(Bound.class, msg -> {
                    log.info("{}", msg);
                    manager.tell(msg, getSelf());
                })
                .match(CommandFailed.class, msg -> getContext().stop(getSelf()))
                .match(Connected.class, conn -> {

                    log.info("" + conn);
                    
                    manager.tell(conn, getSelf());
                    final ActorRef handler = getContext().actorOf(MessageHandlerActor.props());
                    getSender().tell(TcpMessage.register(handler), getSelf());
                })
                .build();
    }

//    @Override
//    public SupervisorStrategy supervisorStrategy() {
//        return new OneForOneStrategy(10, Duration.create("1 minute"), DeciderBuilder
//                .match(Exception.class, e -> SupervisorStrategy.stop())
//                .build());
//    }
}
