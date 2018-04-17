import com.rabbitmq.client.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeoutException;


public class Department {

    private Channel channel;
    private Connection connection;
    private String EXCHANGE_NAME = "mainExchange";
    private List<String> specialisations = Arrays.asList("hip", "knee", "elbow");


    public Department() {
        init();
    }


    private void init(){

        try {
            ConnectionFactory factory = new ConnectionFactory();
            factory.setHost("localhost");
            connection = factory.newConnection();
            channel = connection.createChannel();

            channel.exchangeDeclare(EXCHANGE_NAME, BuiltinExchangeType.TOPIC);

            for(String specialisation: specialisations){
                channel.queueDeclare(specialisation, true, false, false, null).getQueue();
            }

            System.out.println("declared!");

            eventLoop();
            close();


        } catch (IOException e) {
            e.printStackTrace();
        } catch (TimeoutException e) {
            e.printStackTrace();
        }
    }


    private void close(){
        try {
            channel.close();
            connection.close();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (TimeoutException e) {
            e.printStackTrace();
        }
    }

    private void eventLoop() throws IOException{
        while (true) {

            // read msg
            BufferedReader br = new BufferedReader(new InputStreamReader(System.in));

            String message = br.readLine();

            // break condition
            if ("/exit".equals(message) || "/close".equals(message) ) {
                break;
            }

        }
    }

    public static void main(String []args){
        new Department();
    }

}
