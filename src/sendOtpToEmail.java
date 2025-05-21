/*import javax.mail.*;
import javax.mail.internet.*;
import java.util.Properties;
import java.util.Scanner;

public class sendOtpToEmail {

    // Method to send OTP
    public static void sendOtpToEmail(String email, String otp) {
        final String senderEmail = "rengarajanrengarajan5@gmail.com"; // Your email
        final String senderPassword = "jyqc nmtq zrpm ref"; // Use the App Password here, not your Gmail password.

        // Setup mail server properties
        Properties properties = new Properties();
        properties.put("mail.smtp.host", "smtp.gmail.com");
        properties.put("mail.smtp.port", "587");
        properties.put("mail.smtp.auth", "true");
        properties.put("mail.smtp.starttls.enable", "true");

        // Create a session with an authenticator
        Session session = Session.getInstance(properties, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(senderEmail, senderPassword);
            }
        });

        try {
            // Compose the message
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(senderEmail));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(email));
            message.setSubject("Your OTP Code");
            message.setText("Your OTP code is: " + otp);

            // Send the message
            Transport.send(message);
            System.out.println("OTP sent successfully to email: " + email);

        } catch (MessagingException e) {
            // Detailed exception message
            System.out.println("Failed to send OTP. Error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Main method to get email and OTP input from the user
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        // Get email and OTP from user
        System.out.print("Enter recipient email address: ");
        String email = scanner.nextLine();

        System.out.print("Enter OTP: ");
        String otp = scanner.nextLine();

        // Call the method to send OTP
        sendOtpToEmail(email, otp);

        scanner.close();
    }
}*/
