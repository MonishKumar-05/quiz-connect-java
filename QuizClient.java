import java.io.*;
import java.net.*;
import java.util.Scanner;

public class QuizClient {
    public static void main(String[] args) {
        try {

            Socket socket = new Socket("192.168.160.108", 12345);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            Scanner scanner = new Scanner(System.in);


            System.out.print("Enter your name: ");
            String name = scanner.nextLine();
            out.println(name);
            System.out.println("Connected to the quiz server!\n");

            String line;
            while ((line = in.readLine()) != null) {
                if (line.equals("QUESTION")) {

                    String question = "";
                    for (int i = 0; i < 5; i++) {
                        question += in.readLine() + "\n";
                    }
                    System.out.println("\n" + question);

                    // Timer message (e.g. "Time: 15 seconds")
                    String timerMsg = in.readLine();
                    System.out.println(timerMsg);


                    System.out.print("Enter your answer (1-4): ");
                    long start = System.currentTimeMillis();


                    while ((System.currentTimeMillis() - start < 15000) && !scanner.hasNextInt()) {

                    }

                    String answer = "0"; // default
                    if (scanner.hasNextInt()) {
                        answer = scanner.nextLine();
                    }

                    out.println(answer);
                    String feedback = in.readLine();
                    System.out.println(feedback);
                }

                else if (line.equals("LEADERBOARD")) {
                    System.out.println("\n🏆 Leaderboard:");
                    while (!(line = in.readLine()).equals("ENDLEADERBOARD")) {
                        System.out.println(line);
                    }
                }

                else if (line.equals("END")) {
                    System.out.println("\nThanks for playing!");
                    break;
                }


            }

            socket.close();
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
    }
}
