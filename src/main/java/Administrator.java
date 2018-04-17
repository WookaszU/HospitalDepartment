import com.rabbitmq.client.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.UUID;
import java.util.concurrent.TimeoutException;

public class Administrator {

    private String id;
    private Channel channel;
    private Connection connection;
    private String EXCHANGE_NAME = "mainExchange";
    private String myQueue;


    private void generateId(){
        String employeeType = "employee." + this.getClass().getName();
        id = employeeType + "." + UUID.randomUUID().toString();
    }


    public Administrator() {
        generateId();
        init();
        startTask();
    }


    private void startTask(){
        handleCommands();
        close();
    }


    public void init(){
        try {
            // info
            System.out.println("Admin");

            // connection & channel
            ConnectionFactory factory = new ConnectionFactory();
            factory.setHost("localhost");
            connection = factory.newConnection();
            channel = connection.createChannel();

            // exchange
            String EXCHANGE_NAME = "mainExchange";
            channel.exchangeDeclare(EXCHANGE_NAME, BuiltinExchangeType.TOPIC);


            // queue & bind    queue  results   for doctor
            myQueue = channel.queueDeclare(id, true, false, true, null).getQueue();
            channel.queueBind(myQueue, EXCHANGE_NAME, "#");


            // consumer (message handling)
            Consumer consumer = new DefaultConsumer(channel) {
                @Override
                public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws IOException {
                    String message = new String(body, "UTF-8");
                    System.out.println("-------LOG-------");
                    System.out.println("Message: " + message);
                    System.out.println("From: " + properties.getReplyTo());
                }
            };

            channel.basicConsume(myQueue, true, consumer);


        } catch (IOException e) {
            e.printStackTrace();
        } catch (TimeoutException e) {
            e.printStackTrace();
        }
    }


    private void handleCommands(){
        while (true) {

            try {
                // read msg
                BufferedReader br = new BufferedReader(new InputStreamReader(System.in));

                System.out.println("Enter message destination (all/tech/doc) : ");
                String key = br.readLine();

                if(key.equals("all"))
                    key = "employee";
                else if(key.equals("tech"))
                    key = "employee.Technician";
                else if(key.equals("doc"))
                    key = "employee.Doctor";


                System.out.println("Enter message: ");
                String message = br.readLine();

                // break condition
                if ("/exit".equals(message) || "/close".equals(message) ) {
                    break;
                }

                AMQP.BasicProperties properties = new AMQP
                        .BasicProperties()
                        .builder()
                        .correlationId(id)
                        .replyTo(id)
                        .build();

                // publish
                channel.basicPublish(EXCHANGE_NAME, key, properties, message.getBytes("UTF-8"));
                System.out.println("Sent: " + message);
            } catch (IOException e) {
                System.out.println("Unable to send message!");
            }
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


    public static void main(String[] argv) throws Exception {

        new Administrator();
    }
}
