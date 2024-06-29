package com.viettel.vocs.utilities;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class AerospikeBackupParserGpt {

    public static void main(String[] args) {
        try {
            AerospikeBackupParserGpt parser = new AerospikeBackupParserGpt();
            parser.parseBackupFile("/Users/duongnguyen/backup/asbackup.asb");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void parseBackupFile(String filePath) throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(filePath));
        String line;
        
        while ((line = reader.readLine()) != null) {
            if (line.startsWith("Version")) {
                parseHeader(line);
            } else if (line.startsWith("#")) {
                parseMetaData(line);
            } else if (line.startsWith("*")) {
                parseGlobalData(line);
            } else if (line.startsWith("+")) {
                parseRecordHeader(line, reader);
            } else if (line.startsWith("-")) {
                parseBinData(line);
            }
        }

        reader.close();
    }


    private void parseRecordHeader(BufferedReader reader) throws IOException {
        String line;
        while ((line = reader.readLine()) != null && line.startsWith("+")) {
            if (line.startsWith("+ k ")) {
                parseKeyValue(line);
            } else if (line.startsWith("+ n ")) {
                parseNamespace(line);
            } else if (line.startsWith("+ d ")) {
                parseDigest(line);
            } else if (line.startsWith("+ s ")) {
                parseSet(line);
            } else if (line.startsWith("+ g ")) {
                parseGeneration(line);
            } else if (line.startsWith("+ t ")) {
                parseExpiration(line);
            } else if (line.startsWith("+ b ")) {
                parseBinCount(line);
            }
        }

        // After record headers, we should expect bin data lines
        while (line != null && line.startsWith("-")) {
            parseBinData(line);
            line = reader.readLine();
        }
    }

    private void parseKeyValue(String line) {
        // Parse key value line
        System.out.println("Key Value: " + line);
        // Implement parsing logic based on the key type (I, D, S, B)
        // Example: + k I 1234567890
    }

    private void parseNamespace(String line) {
        // Parse namespace line
        System.out.println("Namespace: " + line);
        // Example: + n mynamespace
    }

    private void parseDigest(String line) {
        // Parse digest line
        System.out.println("Digest: " + line);
        // Example: + d abcdefghijklmnopqrstuvwxyz
    }

    private void parseSet(String line) {
        // Parse set line
        System.out.println("Set: " + line);
        // Example: + s myset
    }

    private void parseGeneration(String line) {
        // Parse generation count line
        System.out.println("Generation: " + line);
        // Example: + g 123
    }

    private void parseExpiration(String line) {
        // Parse expiration time line
        System.out.println("Expiration: " + line);
        // Example: + t 1234567890
    }

    private void parseBinCount(String line) {
        // Parse bin count line
        System.out.println("Bin Count: " + line);
        // Example: + b 3
    }

    private void parseHeader(String line) {
        // Parse the header line
        System.out.println("Header: " + line);
    }

    private void parseMetaData(String line) {
        // Parse meta data section
        System.out.println("Meta Data: " + line);
    }

    private void parseGlobalData(String line) {
        // Parse global section data (secondary indexes, UDF files)
        System.out.println("Global Data: " + line);
    }

    private void parseRecordHeader(String line, BufferedReader reader) throws IOException {
        // Parse record header
        System.out.println("Record Header: " + line);
        
        String nextLine;
        List<String> recordHeaderLines = new ArrayList<>();
        recordHeaderLines.add(line);
        
        while ((nextLine = reader.readLine()) != null && nextLine.startsWith("+")) {
            recordHeaderLines.add(nextLine);
        }
        
        // Process the record header lines
        for (String headerLine : recordHeaderLines) {
            System.out.println("Record Header Line: " + headerLine);
        }

        // Now we expect bin data lines
        while (nextLine != null && nextLine.startsWith("-")) {
            parseBinData(nextLine);
            nextLine = reader.readLine();
        }
    }

    private void parseBinData(String line) {
        try {
            // Parse bin data line
            System.out.println("Bin Data: " + line);

            // Example bin data lines:
            // - name I 12345
            // - name D 123.45
            // - name S 5 hello
            // - name B! 5 abcde
            // - name B 8 YWJjZGVmZw==

            String[] tokens = line.split(" ");
            if (tokens.length < 4) {
                throw new IllegalArgumentException("Invalid bin data line: " + line);
            }

            String binType = tokens[1];
            String binName = tokens[2];
            String binValue = tokens[3];

            switch (binType) {
                case "I":
                    long intValue = Long.parseLong(binValue);
                    System.out.println("Bin Name: " + binName + ", Integer Value: " + intValue);
                    break;
                case "D":
                    double doubleValue = Double.parseDouble(binValue);
                    System.out.println("Bin Name: " + binName + ", Double Value: " + doubleValue);
                    break;
                case "S":
                    int stringLength = Integer.parseInt(binValue);
                    String stringData = tokens[4];
                    System.out.println("Bin Name: " + binName + ", String Value: " + stringData);
                    break;
                case "B":
                    boolean isRaw = binValue.equals("!");
                    int bytesLength = Integer.parseInt(isRaw ? tokens[4] : binValue);
                    String bytesData = isRaw ? tokens[5] : tokens[4];
                    System.out.println("Bin Name: " + binName + ", Bytes Value: " + bytesData);
                    break;
                default:
                    throw new IllegalArgumentException("Unknown bin type: " + binType);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}