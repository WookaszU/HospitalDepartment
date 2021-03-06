import com.rabbitmq.client.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.TimeoutException;

public class Technician {


    private String id;
    private Channel channel;
    private Connection connection;
    private String EXCHANGE_NAME = "mainExchange";

    private List<String> specialisations = new ArrayList<String>();


    public Technician() {
        generateId();
        init();
        startTask();
    }


    private void startTask(){
        handleCommands();
        close();
    }


    private void generateId(){
        String employeeType = "employee." + this.getClass().getName();
        id = employeeType + "." + UUID.randomUUID().toString();
    }


    private void askForSpecs() throws IOException{
        // read msg
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));

        int len;
        String[] args;
        do {
            System.out.println("Enter specialisations: ");
            args = br.readLine().split(" ");
            len = args.length;
        }while(len < 2);

        String spec1 = args[0];
        String spec2 = args[1];
        specialisations.add(spec1);
        specialisations.add(spec2);

        //jesli tutaj tworzymy, to gdy nie bylo technikow wczesniej to nic nie dojdzie jak jakis sie pojawi / department
        channel.queueDeclare(spec1, true, false, false, null).getQueue();
        channel.queueDeclare(spec2, true, false, false, null).getQueue();

        channel.queueBind(spec1, EXCHANGE_NAME, spec1 + ".#");
        channel.queueBind(spec2, EXCHANGE_NAME, spec2 + ".#");
    }


    private void init(){

        try {
            // info
            System.out.println("Technician is working now...");

            // connection & channel
            ConnectionFactory factory = new ConnectionFactory();
            factory.setHost("localhost");
            connection = factory.newConnection();
            channel = connection.createChannel();

            // exchange
            channel.exchangeDeclare(EXCHANGE_NAME, BuiltinExchangeType.TOPIC);

            // queue & bind     kolejka dla kazdego technika na wiadomosci dla niego
            String queueName = channel.queueDeclare(id, true, false, true, null).getQueue();
            channel.queueBind(queueName, EXCHANGE_NAME, id);
            channel.queueBind(queueName, EXCHANGE_NAME, "employee.Technician");
            channel.queueBind(queueName, EXCHANGE_NAME, "employee");

            askForSpecs();

            // consumer (message handling)
            Consumer consumer = new DefaultConsumer(channel) {
                @Override
                public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws IOException {

                    String message = new String(body, "UTF-8");
                    System.out.println("Received: " + message + "  from: " + properties.getReplyTo());

                    makeAnalysis();

                    AMQP.BasicProperties replyProperties = new AMQP
                            .BasicProperties()
                            .builder()
                            .correlationId(properties.getCorrelationId())
                            .replyTo(id)
                            .build();

                    message = message + " done";

                    if(properties.getReplyTo().contains("Doctor"))
                        channel.basicPublish(EXCHANGE_NAME, properties.getReplyTo(), replyProperties,
                                message.getBytes("UTF-8"));


                }
            };

            // start listening
            channel.basicConsume(queueName, true, consumer);

            for(String specialisation: specialisations){
                channel.basicConsume(specialisation, true, consumer);
            }


        } catch (IOException e) {
            e.printStackTrace();
        } catch (TimeoutException e) {
            e.printStackTrace();
        }

    }


    private void makeAnalysis(){

        try {
            Random random = new Random();
            Thread.sleep(random.nextInt(10) *100);
        } catch (InterruptedException e) {
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

    private void handleCommands() {
        while (true) {

            try {
                // read msg
                BufferedReader br = new BufferedReader(new InputStreamReader(System.in));

                String message = br.readLine();

                // break condition
                if ("/exit".equals(message) || "/close".equals(message) ) {
                    break;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
    }

    public static void main(String []args){
        new Technician();
    }


}
