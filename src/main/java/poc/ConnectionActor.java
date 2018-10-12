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
        manager.tell(TcpMessage.bind(getSelf(), new InetSocketAddress("localhost", 9099), 100), getSelf());
    }

    @Override
    public void postRestart(Throwable arg0) throws Exception {
        getContext().stop(getSelf());
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(Bound.class, msg -> {
                    log.info("Listening on [{}]", msg.localAddress());
                })
                .match(CommandFailed.class, failed -> {
                    if (failed.cmd() instanceof Tcp.Bind) {
                        log.warning("Cannot bind to [{}]", ((Tcp.Bind) failed.cmd()).localAddress());
                        getContext().stop(getSelf());
                    }
                })
                .match(Connected.class, conn -> {
                    log.info("Received connection from [{}]", conn.remoteAddress());
                    ActorRef connection = getSender();
                    ActorRef handler = getContext().actorOf(MessageHandlerActor.props(connection, conn.remoteAddress()));
                    connection.tell(TcpMessage.register(handler, true, true), getSelf());
                })
                .build();
    }
}
