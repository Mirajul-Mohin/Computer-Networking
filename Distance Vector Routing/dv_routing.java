
import java.io.*;
import java.net.*;
import java.util.*;
import static java.lang.System.exit;

/**
 *
 * @author mirajul
 */

public class dv_routing {

    static int source_port;
    static char source;
    static int totalLine;
    static BufferedReader inp;
    static double linkCost[] = new double[26];
    static String SenderIP= "127.0.0.1";
    static String ReceiverIP= "127.0.0.1";
    static  int MAX_TIME= 90000;
    static  double MAX_DIS= 1000000000.0;


    static void updatingObject(dv_Send snd) throws IOException{
        for (int i = 0; i < totalLine; i++) {
            String eachLine[] = inp.readLine().split(" ");
            snd.update(eachLine);
            char neighbour = eachLine[0].charAt(0);
            linkCost[neighbour - 65] = Double.parseDouble(eachLine[1]);
        }
    }

    static void printDV(List<Packet_Format> distanceVector)
    {
        for (int i = 0; i < distanceVector.size(); i++) {
            if (distanceVector.get(i).distance == MAX_DIS || distanceVector.get(i).source == distanceVector.get(i).destination) {
                continue;
            }
            System.out.println(distanceVector.get(i));
        }
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        System.out.print("Taken Arguments: ");
        for (int i = 0; i < args.length; i++) {
            System.out.print(args[i]);
            System.out.print(" ");
        }
        System.out.println("\nWaiting for updating Distance Vector...");

        source = args[0].charAt(0);                                                              //Retrieving source from command line argument
        source_port = Integer.parseInt(args[1]);                                                        //Retrieving port from command line argument

        inp = new BufferedReader(new FileReader(args[2]));
        totalLine = Integer.parseInt(inp.readLine());

        DatagramSocket datagramSocket = new DatagramSocket(source_port);                               //Creating Datagram Socket Using Port
        List<Packet_Format> distanceVector = new ArrayList<>();

        for (int i = 0; i < 26; i++) {
            linkCost[i] = MAX_DIS;
            if(( i+65) == source)
            {
                distanceVector.add(new Packet_Format(source, (char) (i + 65), (char) (i + 65), 0.0, SenderIP, ReceiverIP));                //Initializing distance Vector
            }
            else
                distanceVector.add(new Packet_Format(source, (char) (i + 65), (char) (i + 65), MAX_DIS, SenderIP, ReceiverIP));
        }


        dv_Send send = new dv_Send(datagramSocket, source, distanceVector,linkCost);                            //Creating Object for sending  packets
        dv_Receive rcev = new dv_Receive(datagramSocket, source, distanceVector, linkCost);                     //Creating Object for Receiving packets


        updatingObject(send);                                           //Updating distance vector using input file


        send.start();                                                   //Starting sending thread
        new Thread(rcev).start();                                       //Starting Receiving thread


        send.join(MAX_TIME);                                             //Waiting for sending thread to finish
        new Thread(rcev).join(MAX_TIME);                                 //Waiting for Receiving thread to finish


        printDV(distanceVector);                                        //Printing the distance vector after finishing updates

        exit(0);
    }




}


class dv_Send extends Thread{

    private DatagramSocket datagramSocket;
    char source;
    double[] linkcost;
    String SenderIp="127.0.0.1" ;
    String ReceiverIp="127.0.0.1";

    List<Packet_Format> distanceVector;

    HashMap<Character, Integer> neighboursNode = new HashMap<>();

    public dv_Send(DatagramSocket datagramSocket, char source, List<Packet_Format> distanceVector,double[] linkcost) {           //This constructor creates an object to send to the neighbours
        this.datagramSocket = datagramSocket;
        this.source = source;
        this.distanceVector = distanceVector;
    }

    void update(String line[]) {                                                   //Method for updating distance vector of Each nodes
        char destination = line[0].charAt(0);
        distanceVector.set(destination - 65, new Packet_Format(source, destination, destination, Double.parseDouble(line[1]), SenderIp,ReceiverIp));
        neighboursNode.put(destination, Integer.parseInt(line[2]));
    }

    public void run() {
        while (true) {

            for (char adjacent : neighboursNode.keySet()) {                            //Loop continues until all neighbours get the distance vector of a Node
                try {
                    InetAddress inetAddress = InetAddress.getByName("127.0.0.1");
                    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                    ObjectOutputStream outputStream = new ObjectOutputStream(byteArrayOutputStream);
                    outputStream.writeObject(distanceVector);

                    byte[] dataForSent = byteArrayOutputStream.toByteArray();
                    DatagramPacket datagramPacket = new DatagramPacket(dataForSent, dataForSent.length, inetAddress, neighboursNode.get(adjacent));        //Forming packets to send data
                    datagramSocket.send(datagramPacket);            //Sending the distance vector to the neighbour

                }
                catch (Exception e) {
                    e.printStackTrace();
                }
            }

            try {
                sleep(5000);                                //Waiting for 5sec before sending distance vector to the neighbours
            } catch (Exception e) {
            }
        }
    }

}


class dv_Receive implements Runnable {

    DatagramSocket datagramSocket;
    List<Packet_Format> distanceVector;
    double linkCost[];
    char source;
    String SenderIp="127.0.0.1" ;
    String ReceiverIp="127.0.0.1";

    public dv_Receive(DatagramSocket datagramSocket, char source, List<Packet_Format> distanceVector, double linkCost[]) {          //This constructor sets a format to update the distance vector
        this.datagramSocket = datagramSocket;
        this.source = source;
        this.distanceVector = distanceVector;
        this.linkCost = linkCost;
    }

    public void run() {
        while (true) {
            byte[] bytes = new byte[1024];
            DatagramPacket datagramPacket = new DatagramPacket(bytes, 1024);
            try {
                datagramSocket.receive(datagramPacket);                                     //Receiving the distance vector form the neighbour
                byte[] data = datagramPacket.getData();
                ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(data);
                ObjectInputStream objectInputStream = new ObjectInputStream(byteArrayInputStream);
                List<Packet_Format> neighboursDistanceVector = (List<Packet_Format>) objectInputStream.readObject();


                for (int i = 0; i < distanceVector.size(); i++) {

                    double dist = neighboursDistanceVector.get(i).distance;
                    char neighbour = neighboursDistanceVector.get(i).source;
                    char dest = neighboursDistanceVector.get(i).destination;
                    if (linkCost[neighbour - 65] + dist < distanceVector.get(dest - 65).distance) {        //Updating the distance vector from neighbour nodes response
                        distanceVector.set(dest - 65, new Packet_Format(source, dest, neighbour, linkCost[neighbour - 65] + dist, SenderIp,ReceiverIp));
                    }
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}



class Packet_Format implements Serializable {                //Implementing serializable interface to exchanging objects
    char source;
    char destination;
    char nextHop;
    double distance;
    String SenderIp,ReceiverIp;

    public Packet_Format(char source, char destination, char nextHop, double distance, String SenderIp, String ReceiverIp) {
        this.source = source;
        this.destination = destination;
        this.nextHop = nextHop;
        this.distance = distance;
        this.SenderIp=SenderIp;
        this.ReceiverIp=ReceiverIp;                                   //This constructor sets the packet format of Sending and Receiving Nodes
    }

    public String toString() {
        return "shortest path to node "+ destination+": "+ "the next hop is "+nextHop+ " and the cost is "+distance;
    }
}
