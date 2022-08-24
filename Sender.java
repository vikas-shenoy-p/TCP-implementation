package Working;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

/*
 Project by: Prajna Gururaj Puranik and Vikas Shenoy Pete
*/

//Client
public class Sender {

    Socket sender;

    ObjectOutputStream out;
    ObjectInputStream in;

    String pkt;
    int SeqNum = 1;
    int ackNum = 0;
    int missingSeq = 0;
    int slideWin = 1;
    boolean flag = false;
    int SegNum=1;
    int randomNumber=0;
    int c=0;
    int array[] = { 0,4097,21505,24577,5121,31745,14337,49153,41985,28673,58369,37889,20481,16385,44033,32769};

    //send window size/number of packets that server should expect in a window
    void sendFrameSize() throws IOException {
        String win = String.valueOf(slideWin);
        out.writeObject(win);
        System.out.println("Window size  " + slideWin);
        out.flush();
    }

    void generateRandom(){
        randomNumber = (int)(Math.random() * (16));//convert number to a multiple of (1024+1)
    }

    public void sendFrames() throws IOException {

        //to test packet loss, uncomment the below function call
        //simulatePacketLoss();

        pkt = String.valueOf(SeqNum);
        out.writeObject(pkt);
        System.out.println("Sent:  " + SeqNum);
        System.out.println("Segment number: " + SegNum);
        out.flush();
        SeqNum=SeqNum+1024;
        SegNum++;
    }

    //code to test packet loss by randomly choosing a sequence number and dropping it
    private void simulatePacketLoss(){
        generateRandom();
        System.out.println(randomNumber);
        if(SeqNum == array[randomNumber]){
            SeqNum+=1024;
            c+=1;
        }
    }

    //send the oldest unacknowledged packet in a window
    private void sendLostFrame(int lostSeq) throws IOException {

        System.out.println("-----sending lost packet!-------------");
        pkt = String.valueOf(lostSeq);
        out.writeObject(pkt);
        int lost = Integer.parseInt(pkt);
        System.out.println("Sent  " + lost);
        out.flush();
        flag = false;
        slideWin +=1;
        SegNum++;
    }

    //calculate the next sequence number
    int getNextSeqNumber(){
        return SeqNum;
    }


    public void run() throws IOException, ClassNotFoundException {

        //to connect to server running on the same computer
        //sender = new Socket("localhost", 1500);

        //to connect to server running on a different computer
        sender = new Socket("192.168.103.124", 1500);

        out = new ObjectOutputStream(sender.getOutputStream());
        in = new ObjectInputStream(sender.getInputStream());

        while (SegNum<=Math.pow(10,7)){ // For 10000000 segments
            System.out.println("---------------------------------");
            sendFrameSize();

            int loopCount=0;

            while ((loopCount < slideWin) && SegNum<=Math.pow(10,7)) {

                //if sliding window has reached max value, reset sequence number to 1
                if (SeqNum >= Math.pow(2,16)+1){
                    SeqNum=1;
                }

                sendFrames();
                receiveACK();
                detectPacketLoss();
                loopCount++;
            }

            //Modify the sliding window
            if((!flag)){ //if packet loss is not detected
                if ((slideWin*2)<=(Math.pow(2,16)+1)){slideWin *= 2; }
                else {continue;}
            }
            else{   //if packet loss has occurred, resend the lost packet and modify sliding window
                //next packet to be sent is packet with the received ACK no
                if(slideWin > 1)
                    slideWin = slideWin / 2;
                sendLostFrame(missingSeq);
            }
        }

        in.close();
        out.close();
        sender.close();
        System.out.println("\nConnection Terminated");
    }

    //Detect packet loss using below function
    private void detectPacketLoss() {
        if(SeqNum > ackNum){
            if(!flag) {
                missingSeq = ackNum;
                flag = true;
            }
        }
    }

    //Register the acknowledged received from the server
    private void receiveACK() {
        try {
            String Ack = (String) in.readObject();
            ackNum = Integer.parseInt(Ack);
            System.out.println("ACK received : " + ackNum);
        }catch (Exception e) {}
    }

    public static void main(String args[]) throws IOException, ClassNotFoundException {
        Sender s = new Sender();

        try {
            s.run();
        }catch(IOException e){
            System.out.println("\nReciever stopped!");
        }
    }
}
