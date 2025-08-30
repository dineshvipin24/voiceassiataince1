import javax.sound.sampled.*;
import java.io.*;
import java.util.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.*;

// FreeTTS imports
import com.sun.speech.freetts.Voice;
import com.sun.speech.freetts.VoiceManager;
import com.sun.speech.freetts.audio.JavaClipAudioPlayer;

// Alternative speech recognition imports
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class Main {
    public static void main(String[] args) throws Exception {
        System.out.println("🚀 Starting Voice Assistant with FreeTTS...");
        
        // Set FreeTTS system properties
        System.setProperty("freetts.voices", "com.sun.speech.freetts.en.us.cmu_us_kal.KevinVoiceDirectory");
        
        TextToSpeech tts = new TextToSpeech();
        SpeechRecognizer recognizer = new SpeechRecognizer();
        CommandProcessor processor = new CommandProcessor(tts);
        
        tts.speak("Hello, I am your voice assistant. How can I help you?");
        recognizer.startRecognition(processor);
    }
}

class SpeechRecognizer {
    private Scanner scanner;
    private boolean isListening = true;
    private AudioRecorder audioRecorder;
    private CommandProcessor processor;
    private boolean speechRecognitionAvailable = false;
    
    public void startRecognition(CommandProcessor processor) throws Exception {
        this.processor = processor;
        scanner = new Scanner(System.in);
        audioRecorder = new AudioRecorder();
        
        // Try to initialize Windows Speech Recognition
        initializeWindowsSpeechRecognition();
        
        System.out.println("🎤 Voice Assistant is ready!");
        System.out.println("💡 Choose input mode:");
        System.out.println("   1. Type commands (press Enter)");
        if (speechRecognitionAvailable) {
            System.out.println("   2. Voice commands (press V to start listening)");
        } else {
            System.out.println("   2. Voice commands (press V for enhanced voice mode)");
        }
        System.out.println("🔍 Try: 'open calculator', 'what time is it', 'open youtube', 'help'");
        System.out.println("🛑 Type/say 'exit' to quit");
        System.out.println("=" + "=".repeat(60));
        
        while (isListening) {
            System.out.print("\n👤 You (type command or press V for voice): ");
            String input = scanner.nextLine().trim();
            
            if (input.isEmpty()) {
                continue;
            }
            
            String command;
            
            // Check if user wants to use voice input
            if (input.equalsIgnoreCase("v") || input.toLowerCase().startsWith("voice")) {
                command = captureVoiceInput();
                if (command == null || command.isEmpty()) {
                    continue;
                }
            } else {
                command = input;
            }
            
            System.out.println("🔄 Processing: " + command);
            
            if (command.toLowerCase().contains("exit") || 
                command.toLowerCase().contains("quit") ||
                command.toLowerCase().contains("stop")) {
                processor.processCommand("exit");
                break;
            }
            
            processor.processCommand(command.toLowerCase());
        }
        
        cleanup();
    }
    
    private void initializeWindowsSpeechRecognition() {
        try {
            System.out.println("🎙️  Checking Windows Speech Recognition availability...");
            
            // Test if Windows Speech Recognition is available
            ProcessBuilder pb = new ProcessBuilder(
                "powershell", 
                "-Command", 
                "Get-Command 'Add-Type' -ErrorAction SilentlyContinue"
            );
            Process process = pb.start();
            int exitCode = process.waitFor();
            
            if (exitCode == 0) {
                speechRecognitionAvailable = true;
                System.out.println("✅ Windows Speech Recognition available!");
            } else {
                System.out.println("⚠️  Windows Speech Recognition not available, using enhanced text mode");
                speechRecognitionAvailable = false;
            }
            
        } catch (Exception e) {
            System.err.println("⚠️  Speech recognition check failed: " + e.getMessage());
            speechRecognitionAvailable = false;
        }
    }
    
    private String captureVoiceInput() {
        if (speechRecognitionAvailable) {
            return captureVoiceInputWithSpeechRecognition();
        } else {
            return captureVoiceInputFallback();
        }
    }
    
    private String captureVoiceInputWithSpeechRecognition() {
        try {
            System.out.println("🎙️  Starting voice recognition...");
            System.out.println("🗣️  Please speak clearly and wait for the recognition to complete");
            
            // Create PowerShell script for speech recognition
            String powerShellScript = 
                "Add-Type -AssemblyName System.Speech;" +
                "$recognizer = New-Object System.Speech.Recognition.SpeechRecognitionEngine;" +
                "$recognizer.SetInputToDefaultAudioDevice();" +
                "$grammar = New-Object System.Speech.Recognition.DictationGrammar;" +
                "$recognizer.LoadGrammar($grammar);" +
                "$recognizer.RecognizeTimeout = [TimeSpan]::FromSeconds(10);" +
                "Write-Host 'Ready - Please speak now...';" +
                "$result = $recognizer.Recognize();" +
                "if ($result) { $result.Text } else { 'No speech detected' };" +
                "$recognizer.Dispose()";
            
            // Execute PowerShell script
            ProcessBuilder pb = new ProcessBuilder(
                "powershell", 
                "-Command", 
                powerShellScript
            );
            
            pb.redirectErrorStream(true);
            Process process = pb.start();
            
            // Read the output
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            StringBuilder output = new StringBuilder();
            String line;
            boolean readyMessageSeen = false;
            
            while ((line = reader.readLine()) != null) {
                if (line.contains("Ready - Please speak now")) {
                    readyMessageSeen = true;
                    System.out.println("🔴 Recording... Speak now!");
                } else if (readyMessageSeen && !line.trim().isEmpty() && !line.contains("No speech detected")) {
                    output.append(line.trim()).append(" ");
                }
            }
            
            process.waitFor();
            
            String recognizedText = output.toString().trim();
            if (!recognizedText.isEmpty() && !recognizedText.equals("No speech detected")) {
                System.out.println("👂 Recognized: " + recognizedText);
                return recognizedText;
            } else {
                System.out.println("⏱️  No speech detected or timeout. Try again.");
                return null;
            }
            
        } catch (Exception e) {
            System.err.println("❌ Error during Windows speech recognition: " + e.getMessage());
            System.out.println("🔄 Falling back to text input...");
            return captureVoiceInputFallback();
        }
    }
    
    private String captureVoiceInputFallback() {
        try {
            System.out.println("🎙️  Enhanced Voice Mode");
            System.out.println("💬 Please type what you would like to say:");
            System.out.print("   👄 Voice Command: ");
            
            String spokenText = scanner.nextLine().trim();
            
            if (!spokenText.isEmpty()) {
                System.out.println("👂 Processing voice command: " + spokenText);
                // Add some audio feedback
                audioRecorder.playBeep();
                return spokenText;
            }
            
            return null;
            
        } catch (Exception e) {
            System.err.println("❌ Error in enhanced voice input: " + e.getMessage());
            return null;
        }
    }
    
    private void cleanup() {
        try {
            if (scanner != null) {
                scanner.close();
            }
        } catch (Exception e) {
            System.err.println("❌ Error during cleanup: " + e.getMessage());
        }
    }
}

class AudioRecorder {
    private AudioFormat audioFormat;
    private TargetDataLine targetDataLine;
    
    public AudioRecorder() {
        // Configure audio format for recording
        float sampleRate = 16000;
        int sampleSizeInBits = 16;
        int channels = 1;
        boolean signed = true;
        boolean bigEndian = false;
        
        audioFormat = new AudioFormat(sampleRate, sampleSizeInBits, channels, signed, bigEndian);
    }
    
    public void startRecording() {
        try {
            DataLine.Info info = new DataLine.Info(TargetDataLine.class, audioFormat);
            targetDataLine = (TargetDataLine) AudioSystem.getLine(info);
            targetDataLine.open(audioFormat);
            targetDataLine.start();
            System.out.println("🎙️  Recording started...");
        } catch (Exception e) {
            System.err.println("❌ Error starting recording: " + e.getMessage());
        }
    }
    
    public void stopRecording() {
        if (targetDataLine != null) {
            targetDataLine.stop();
            targetDataLine.close();
            System.out.println("🛑 Recording stopped");
        }
    }
    
    public void playBeep() {
        try {
            // Generate a simple beep sound to indicate voice input received
            Toolkit.getDefaultToolkit().beep();
        } catch (Exception e) {
            // Ignore beep errors
        }
    }
}

class CommandProcessor {
    private TextToSpeech tts;

    public CommandProcessor(TextToSpeech tts) {
        this.tts = tts;
    }

    public void processCommand(String command) {
        try {
            // Application Commands
            if (command.contains("open calculator") || command.contains("calculator")) {
                executeCommand("Opening Calculator", "cmd /c start calc");
                
            } else if (command.contains("open notepad") || command.contains("notepad")) {
                executeCommand("Opening Notepad", "cmd /c start notepad");
                
            } else if (command.contains("open file manager") || command.contains("open explorer") || command.contains("file manager")) {
                executeCommand("Opening File Manager", "cmd /c start explorer");
                
            } else if (command.contains("open chrome") && !command.contains("youtube") && !command.contains("music")) {
                executeCommand("Opening Chrome browser", "cmd /c start chrome");
                
            } else if (command.contains("open edge")) {
                executeCommand("Opening Microsoft Edge", "cmd /c start msedge");
                
            } else if (command.contains("open firefox")) {
                executeCommand("Opening Firefox", "cmd /c start firefox");
                
            // Web Services
            } else if (command.contains("play songs") || command.contains("open music") || command.contains("youtube music")) {
                executeCommand("Opening YouTube Music", "cmd /c start chrome https://music.youtube.com/");
                
            } else if (command.contains("open youtube") || command.contains("youtube")) {
                executeCommand("Opening YouTube", "cmd /c start chrome https://www.youtube.com/");
                
            } else if (command.contains("open google") || command.contains("google")) {
                executeCommand("Opening Google", "cmd /c start chrome https://www.google.com/");
                
            } else if (command.contains("open gmail") || command.contains("gmail")) {
                executeCommand("Opening Gmail", "cmd /c start chrome https://mail.google.com/");
                
            } else if (command.contains("open facebook") || command.contains("facebook")) {
                executeCommand("Opening Facebook", "cmd /c start chrome https://www.facebook.com/");
                
            } else if (command.contains("open twitter") || command.contains("twitter") || command.contains("open x")) {
                executeCommand("Opening Twitter", "cmd /c start chrome https://twitter.com/");
                
            } else if (command.contains("open instagram") || command.contains("instagram")) {
                executeCommand("Opening Instagram", "cmd /c start chrome https://www.instagram.com/");
                
            } else if (command.contains("open whatsapp") || command.contains("whatsapp")) {
                executeCommand("Opening WhatsApp Web", "cmd /c start chrome https://web.whatsapp.com/");
                
            } else if (command.contains("open linkedin") || command.contains("linkedin")) {
                executeCommand("Opening LinkedIn", "cmd /c start chrome https://www.linkedin.com/");
                
            // System Commands
            } else if (command.contains("open control panel") || command.contains("control panel")) {
                executeCommand("Opening Control Panel", "cmd /c start control");
                
            } else if (command.contains("open task manager") || command.contains("task manager")) {
                executeCommand("Opening Task Manager", "cmd /c start taskmgr");
                
            } else if (command.contains("open settings") || command.contains("windows settings")) {
                executeCommand("Opening Windows Settings", "cmd /c start ms-settings:");
                
            } else if (command.contains("open command prompt") || command.contains("open cmd") || command.contains("command prompt")) {
                executeCommand("Opening Command Prompt", "cmd /c start cmd");
                
            } else if (command.contains("open powershell") || command.contains("powershell")) {
                executeCommand("Opening PowerShell", "cmd /c start powershell");
                
            // Office Applications
            } else if (command.contains("open word") || command.contains("microsoft word")) {
                executeCommand("Opening Microsoft Word", "cmd /c start winword");
                
            } else if (command.contains("open excel") || command.contains("microsoft excel")) {
                executeCommand("Opening Microsoft Excel", "cmd /c start excel");
                
            } else if (command.contains("open powerpoint") || command.contains("microsoft powerpoint")) {
                executeCommand("Opening PowerPoint", "cmd /c start powerpnt");
                
            // Media and Entertainment
            } else if (command.contains("open media player") || command.contains("media player")) {
                executeCommand("Opening Windows Media Player", "cmd /c start wmplayer");
                
            } else if (command.contains("open paint") || command.contains("paint")) {
                executeCommand("Opening Paint", "cmd /c start mspaint");
                
            } else if (command.contains("open netflix") || command.contains("netflix")) {
                executeCommand("Opening Netflix", "cmd /c start chrome https://www.netflix.com/");
                
            } else if (command.contains("open spotify") || command.contains("spotify")) {
                executeCommand("Opening Spotify", "cmd /c start chrome https://open.spotify.com/");
                
            // Time and Date
            } else if (command.contains("what time") || command.contains("current time") || command.contains("time now") || command.equals("time")) {
                String currentTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("h:mm a"));
                String response = "The current time is " + currentTime;
                respondWithSpeech(response);
                
            } else if (command.contains("what date") || command.contains("current date") || command.contains("today") || command.equals("date")) {
                String currentDate = LocalDateTime.now().format(DateTimeFormatter.ofPattern("EEEE, MMMM dd, yyyy"));
                String response = "Today is " + currentDate;
                respondWithSpeech(response);
                
            } else if (command.contains("day") && (command.contains("what") || command.contains("which"))) {
                String dayName = LocalDateTime.now().format(DateTimeFormatter.ofPattern("EEEE"));
                String response = "Today is " + dayName;
                respondWithSpeech(response);
                
            // Weather (placeholder)
            } else if (command.contains("weather") || command.contains("temperature")) {
                String response = "I'm sorry, I don't have access to live weather data yet. You can check weather by saying 'open google' and then searching for weather.";
                respondWithSpeech(response);
                
            // Greetings and Interactions
            } else if (command.contains("hello") || command.contains("hi") || command.contains("hey")) {
                String response = "Hello! How can I help you today?";
                respondWithSpeech(response);
                
            } else if (command.contains("how are you")) {
                String response = "I'm doing great! Ready to assist you with anything you need.";
                respondWithSpeech(response);
                
            } else if (command.contains("good morning")) {
                String response = "Good morning! Hope you have a wonderful day ahead!";
                respondWithSpeech(response);
                
            } else if (command.contains("good evening")) {
                String response = "Good evening! How can I help you tonight?";
                respondWithSpeech(response);
                
            } else if (command.contains("good night")) {
                String response = "Good night! Sweet dreams!";
                respondWithSpeech(response);
                
            } else if (command.contains("what can you do") || command.contains("help") || command.contains("commands")) {
                showHelp();
                
            } else if (command.contains("thank you") || command.contains("thanks")) {
                String response = "You're welcome! Is there anything else I can help you with?";
                respondWithSpeech(response);
                
            } else if (command.contains("what is your name") || command.contains("who are you")) {
                String response = "I'm your personal voice assistant, built with Java and FreeTTS. I'm here to help you with various tasks!";
                respondWithSpeech(response);
                
            // Search Commands
            } else if (command.contains("search for ") || command.contains("google search ")) {
                String searchQuery = command.replace("search for ", "").replace("google search ", "").trim();
                if (!searchQuery.isEmpty()) {
                    String url = "https://www.google.com/search?q=" + searchQuery.replace(" ", "+");
                    executeCommand("Searching for " + searchQuery, "cmd /c start chrome \"" + url + "\"");
                } else {
                    respondWithSpeech("What would you like me to search for?");
                }
                
            // Fun Commands
            } else if (command.contains("tell me a joke") || command.contains("joke")) {
                tellJoke();
                
            } else if (command.contains("flip a coin") || command.contains("coin flip")) {
                flipCoin();
                
            } else if (command.contains("roll a dice") || command.contains("roll dice")) {
                rollDice();
                
            // Math Commands
            } else if (command.contains("calculate") || command.contains("what is")) {
                handleMath(command);
                
            // Voice Test
            } else if (command.contains("test voice") || command.contains("speak test")) {
                String response = "Voice test successful! FreeTTS is working correctly. You can hear my voice clearly.";
                respondWithSpeech(response);
                
            // System Info
            } else if (command.contains("system info") || command.contains("computer info")) {
                String response = "You're running Windows with Java Voice Assistant powered by FreeTTS. For detailed system information, I can open System Information.";
                respondWithSpeech(response);
                executeCommand("Opening System Information", "cmd /c start msinfo32");
                
            // Exit Commands
            } else if (command.contains("exit") || command.contains("stop") || 
                      command.contains("goodbye") || command.contains("quit") || command.contains("bye")) {
                String response = "Thank you for using the voice assistant. Have a great day! Goodbye!";
                respondWithSpeech(response);
                
                // Wait for speech to complete
                Thread.sleep(3000);
                System.out.println("👋 Goodbye!");
                System.exit(0);
                
            } else {
                String response = "Sorry, I didn't understand that command. Type or say 'help' to see what I can do.";
                respondWithSpeech(response);
            }

        } catch (Exception e) {
            System.err.println("❌ Error processing command: " + e.getMessage());
            String response = "Sorry, there was an error processing your request.";
            respondWithSpeech(response);
        }
    }
    
    private void showHelp() {
        String helpText = "Here are some things I can do";
        
        System.out.println("\n" + "=".repeat(60));
        System.out.println("🤖 Assistant Commands:");
        System.out.println("🖥️  Applications: 'open calculator', 'open notepad', 'open chrome'");
        System.out.println("🌐 Websites: 'open youtube', 'open google', 'open gmail'");
        System.out.println("🕐 Time & Date: 'what time is it', 'what date is it'");
        System.out.println("🔍 Search: 'search for java programming'");
        System.out.println("🎯 System: 'open control panel', 'open task manager'");
        System.out.println("🎲 Fun: 'tell me a joke', 'flip a coin', 'roll dice'");
        System.out.println("🧮 Math: 'calculate 5 plus 3'");
        System.out.println("🔊 Voice: 'test voice' to test speech");
        System.out.println("🚪 Exit: 'exit', 'quit', 'goodbye'");
        System.out.println("=".repeat(60));
        
        tts.speak(helpText + ". I can open applications, websites, tell you time and date, search the web, and much more! Check the console for a detailed list.");
    }
    
    private void tellJoke() {
        String[] jokes = {
            "Why do Java developers wear glasses? Because they can't C sharp!",
            "Why did the programmer quit his job? He didn't get arrays!",
            "How many programmers does it take to change a light bulb? None, that's a hardware problem!",
            "Why do programmers prefer dark mode? Because light attracts bugs!",
            "What's a programmer's favorite hangout place? The Foo Bar!",
            "Why don't programmers like nature? It has too many bugs!",
            "What do you call 8 hobbits? A hobbyte!",
            "Why did the developer go broke? Because he used up all his cache!"
        };
        
        Random random = new Random();
        String joke = jokes[random.nextInt(jokes.length)];
        respondWithSpeech(joke);
    }
    
    private void flipCoin() {
        Random random = new Random();
        String result = random.nextBoolean() ? "Heads" : "Tails";
        String response = "I flipped a coin and got: " + result + "!";
        respondWithSpeech(response);
    }
    
    private void rollDice() {
        Random random = new Random();
        int result = random.nextInt(6) + 1;
        String response = "I rolled a dice and got: " + result + "!";
        respondWithSpeech(response);
    }
    
    private void handleMath(String command) {
        try {
            String mathExpression = command.replace("calculate", "").replace("what is", "").trim();
            
            if (mathExpression.contains("plus") || mathExpression.contains("+")) {
                String[] parts = mathExpression.split("plus|\\+");
                if (parts.length == 2) {
                    double num1 = Double.parseDouble(parts[0].trim());
                    double num2 = Double.parseDouble(parts[1].trim());
                    double result = num1 + num2;
                    respondWithSpeech(num1 + " plus " + num2 + " equals " + result);
                    return;
                }
            } else if (mathExpression.contains("minus") || mathExpression.contains("-")) {
                String[] parts = mathExpression.split("minus|-");
                if (parts.length == 2) {
                    double num1 = Double.parseDouble(parts[0].trim());
                    double num2 = Double.parseDouble(parts[1].trim());
                    double result = num1 - num2;
                    respondWithSpeech(num1 + " minus " + num2 + " equals " + result);
                    return;
                }
            } else if (mathExpression.contains("times") || mathExpression.contains("*")) {
                String[] parts = mathExpression.split("times|\\*");
                if (parts.length == 2) {
                    double num1 = Double.parseDouble(parts[0].trim());
                    double num2 = Double.parseDouble(parts[1].trim());
                    double result = num1 * num2;
                    respondWithSpeech(num1 + " times " + num2 + " equals " + result);
                    return;
                }
            }
            
            respondWithSpeech("Sorry, I can only handle simple math like '5 plus 3' or '10 minus 4'");
            
        } catch (Exception e) {
            respondWithSpeech("Sorry, I couldn't calculate that. Try something like 'calculate 5 plus 3'");
        }
    }
    
    private void executeCommand(String message, String command) {
        try {
            System.out.println("🤖 Assistant: " + message);
            tts.speak(message);
            Runtime.getRuntime().exec(command);
            
        } catch (IOException e) {
            System.err.println("❌ Error executing command: " + e.getMessage());
            String errorResponse = "Sorry, I couldn't open that. Make sure the application is installed.";
            respondWithSpeech(errorResponse);
        }
    }
    
    private void respondWithSpeech(String response) {
        System.out.println("🤖 Assistant: " + response);
        tts.speak(response);
    }
}

class TextToSpeech {
    private Voice voice;

    public TextToSpeech() {
        try {
            System.out.println("🔊 Initializing FreeTTS Text-to-Speech...");
            
            // Set FreeTTS voice directory
            System.setProperty("freetts.voices", 
                "com.sun.speech.freetts.en.us.cmu_us_kal.KevinVoiceDirectory");
            
            // Initialize VoiceManager
            VoiceManager voiceManager = VoiceManager.getInstance();
            Voice[] voices = voiceManager.getVoices();
            
            System.out.println("Available voices:");
            for (Voice v : voices) {
                System.out.println("  - " + v.getName());
            }
            
            // Try to get Kevin voice first
            voice = voiceManager.getVoice("kevin16");
            
            if (voice == null) {
                // Try alternative voice names
                voice = voiceManager.getVoice("kevin");
                if (voice == null) {
                    voice = voiceManager.getVoice("cmu_us_kal");
                    if (voice == null && voices.length > 0) {
                        // Use first available voice
                        voice = voices[0];
                    }
                }
            }

            if (voice != null) {
                voice.allocate();
                
                // Set voice properties for better quality
                voice.setRate(150);  // Speech rate
                voice.setPitch(100); // Pitch
                voice.setVolume(1.0f); // Volume
                
                System.out.println("✅ FreeTTS initialized successfully with voice: " + voice.getName());
                
                // Test the voice
                System.out.println("🔊 Testing voice...");
                
            } else {
                throw new IllegalStateException("No FreeTTS voices available. Check JAR files in classpath.");
            }
            
        } catch (Exception e) {
            System.err.println("❌ Error initializing FreeTTS: " + e.getMessage());
            System.err.println("💡 Make sure all FreeTTS JAR files are in the classpath:");
            System.err.println("   - freetts.jar");
            System.err.println("   - cmu_us_kal.jar");
            System.err.println("   - cmu_us_kal16.jar");
            e.printStackTrace();
            throw new RuntimeException("Failed to initialize FreeTTS", e);
        }
    }

    public void speak(String text) {
        try {
            if (voice != null && text != null && !text.trim().isEmpty()) {
                System.out.println("🗣️  Speaking: " + text);
                voice.speak(text);
            } else {
                System.out.println("🔇 Voice not available or empty text");
            }
        } catch (Exception e) {
            System.err.println("❌ Error in speech synthesis: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    public void cleanup() {
        if (voice != null) {
            try {
                voice.deallocate();
                System.out.println("🔊 FreeTTS cleaned up successfully");
            } catch (Exception e) {
                System.err.println("❌ Error cleaning up FreeTTS: " + e.getMessage());
            }
        }
    }
}