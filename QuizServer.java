import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;


public class QuizServer {
    private static final int PORT = 12345;
    private static final String JOIN_CODE = "123ABC";
    private static final int QUESTION_TIME_LIMIT = 15; // seconds
    private static final int MIN_PLAYERS = 2;

    private static List<ClientHandler> clients = new ArrayList<>();
    private static List<Question> questions = new ArrayList<>();

    public static void main(String[] args) throws IOException {
        loadQuestions(); // Java basics questions

        ServerSocket serverSocket = new ServerSocket(PORT);
        String hostAddress = InetAddress.getLocalHost().getHostAddress();

        System.out.println("\nQuiz Server is running...");
        System.out.println("Join Code: " + JOIN_CODE);
        System.out.println("IP Address: " + hostAddress);
        System.out.println("Waiting for at least 2 players to join...\n");

        ExecutorService pool = Executors.newFixedThreadPool(10);

        new Thread(() -> {
            try {
                while (clients.size() < MIN_PLAYERS) {
                    Thread.sleep(1000);
                }
                System.out.println("Minimum players joined. Starting quiz in 10 seconds...");
                for (int i = 10; i > 0; i--) {
                    System.out.println("Starting in: " + i + " seconds...");
                    Thread.sleep(1000);
                }
                startQuiz();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();

        while (true) {
            Socket socket = serverSocket.accept();
            ClientHandler client = new ClientHandler(socket);
            clients.add(client);
            pool.execute(client);
        }
    }

    private static void loadQuestions() {
        questions.add(new Question("Which keyword is used to define a class in Java?", new String[]{"class", "ClassName", "object", "define"}, 1));
        questions.add(new Question("What is the size of an int variable in Java?", new String[]{"2 bytes", "4 bytes", "8 bytes", "Depends on OS"}, 2));
        questions.add(new Question("Which method is the entry point of any Java program?", new String[]{"start()", "main()", "run()", "init()"}, 2));
        questions.add(new Question("Which of these is not a primitive data type in Java?", new String[]{"int", "boolean", "String", "char"}, 3));
        questions.add(new Question("Which keyword is used to inherit a class in Java?", new String[]{"this", "super", "extends", "implements"}, 3));
        questions.add(new Question("What is the default value of a boolean variable in Java?", new String[]{"true", "false", "0", "null"}, 2));
        questions.add(new Question("Which operator is used for comparison in Java?", new String[]{"=", "==", ":=", "equals"}, 2));
        questions.add(new Question("Which loop checks the condition at the end?", new String[]{"for", "while", "do-while", "foreach"}, 3));
        questions.add(new Question("Which of the following is not a Java keyword?", new String[]{"static", "Boolean", "try", "final"}, 2));
        questions.add(new Question("Which package contains the Scanner class?", new String[]{"java.io", "java.util", "java.lang", "java.awt"}, 2));
    }

    private static void startQuiz() {
        for (Question question : questions) {
            broadcast("QUESTION");
            broadcast(question.toString());
            CountDownLatch latch = new CountDownLatch(clients.size());
            long startTime = System.currentTimeMillis();

            for (ClientHandler client : clients) {
                new Thread(() -> {
                    try {
                        client.send("Time: " + QUESTION_TIME_LIMIT + " seconds");
                        client.socket.setSoTimeout(QUESTION_TIME_LIMIT * 1000);
                        String response = client.in.readLine();
                        long responseTime = System.currentTimeMillis() - startTime;
                        if (response != null && Integer.parseInt(response) == question.correctOption) {
                            int score = Math.max(10, 100 - (int) responseTime / 150);
                            client.score += score;
                            client.send("✅ Correct! +" + score + " points");
                        } else {
                            client.send("❌ Oops! That's incorrect.");
                        }
                    } catch (Exception ignored) {
                        client.send("❌ Oops! That's incorrect or no answer.");
                    } finally {
                        latch.countDown();
                    }
                }).start();
            }

            try {
                latch.await(QUESTION_TIME_LIMIT + 2, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        broadcast("LEADERBOARD");
        clients.sort((a, b) -> b.score - a.score);
        System.out.println("\nFinal Leaderboard:");
        for (int i = 0; i < clients.size(); i++) {
            ClientHandler client = clients.get(i);
            String result = (i + 1) + ". " + client.name + " - " + client.score + " points";
            broadcast(result);
            System.out.println(result);
        }
        broadcast("ENDLEADERBOARD");
        broadcast("END");
    }

    private static void broadcast(String message) {
        for (ClientHandler client : clients) {
            client.send(message);
        }
    }

    private static class ClientHandler implements Runnable {
        private Socket socket;
        private BufferedReader in;
        private PrintWriter out;
        private String name;
        private int score = 0;

        public ClientHandler(Socket socket) {
            this.socket = socket;
        }

        public void run() {
            try {
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out = new PrintWriter(socket.getOutputStream(), true);
                name = in.readLine();
                System.out.println(name + " joined the quiz!");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public void send(String message) {
            out.println(message);
        }
    }

    private static class Question {
        String question;
        String[] options;
        int correctOption;

        public Question(String q, String[] opt, int correct) {
            question = q;
            options = opt;
            correctOption = correct;
        }

        public String toString() {
            return question + "\n1. " + options[0] + "\n2. " + options[1] + "\n3. " + options[2] + "\n4. " + options[3];
        }
    }
}
