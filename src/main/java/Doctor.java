import com.rabbitmq.client.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.UUID;
import java.util.concurrent.TimeoutException;

public class Doctor {


    private String id;
    private Channel channel;
    private String EXCHANGE_NAME = "mainExchange";
    private String resultsQueue;
    private Connection connection;


    public Doctor(String EXCHANGE_NAME) {
        this.EXCHANGE_NAME = EXCHANGE_NAME;
        generateId();
        init();
    }


    private void generateId(){
        String employeeType = "employee." + this.getClass().getName();
        id = employeeType + "." + UUID.randomUUID().toString();
    }


    void init(){

        try {
            // info
            System.out.println("Doctor is working now...");


            // connection & channel
            ConnectionFactory factory = new ConnectionFactory();
            factory.setHost("localhost");
            connection = factory.newConnection();
            channel = connection.createChannel();

            // exchange
            channel.exchangeDeclare(EXCHANGE_NAME, BuiltinExchangeType.TOPIC);


            // queue & bind    queue  results   for doctor
            resultsQueue = channel.queueDeclare(id, true, false, true, null).getQueue();
            channel.queueBind(resultsQueue, EXCHANGE_NAME, id);
            channel.queueBind(resultsQueue, EXCHANGE_NAME, "employee.Doctor");
            channel.queueBind(resultsQueue, EXCHANGE_NAME, "employee");

            // consumer (message handling)
            Consumer consumer = new DefaultConsumer(channel) {
                @Override
                public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws IOException {
                    String message = new String(body, "UTF-8");
                    System.out.println("Received: " + message);
                }
            };

            // start listening
            System.out.println("Waiting for messages...");
            channel.basicConsume(resultsQueue, true, consumer);

            eventLoop();

            close();
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

            System.out.println("Enter KEY (hip / knee / elbow): ");
            String key = br.readLine();

            System.out.println("Enter patient name: ");
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


    public static void main(String []args){

        new Doctor("mainExchange");
    }

}
