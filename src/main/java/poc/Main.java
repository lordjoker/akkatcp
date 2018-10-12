package poc;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.io.Tcp;

public class Main {

    public static void main(String[] args) {
        ActorSystem actorSystem = ActorSystem.create("system");
        ActorRef tcpManager = Tcp.get(actorSystem).getManager();
        actorSystem.actorOf(ConnectionActor.props(tcpManager), "connection");
    }
}
