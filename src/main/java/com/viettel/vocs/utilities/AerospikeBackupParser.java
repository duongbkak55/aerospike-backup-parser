package com.viettel.vocs.utilities;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

public class AerospikeBackupParser {

    public static void main(String[] args) {
        try {
            AerospikeBackupParser parser = new AerospikeBackupParser();
            parser.parseBackupFile("/Users/duongnguyen/backup/asbackup.asb");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void parseBackupFile(String filePath) {
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            String delimiter = " ";
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("Version")) {
                    parseHeader(line);
                    continue;
                }
                if (delimiter == null) {
                    throw new IllegalStateException("Delimiter not found in the header");
                }
                if (line.startsWith("#")) {
                    parseMetaData(line, delimiter);
                } else if (line.startsWith("*")) {
                    parseGlobalData(line, delimiter);
                } else if (line.startsWith("+")) {
                    parseRecordHeader(line, reader, delimiter);
                } else if (line.startsWith("-")) {
                    parseBinData(line, delimiter);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void parseHeader(String line) {
        String[] tokens = line.split(" ", 2);
        if (tokens.length != 2 || !tokens[0].equals("Version")) {
            throw new IllegalArgumentException("Invalid header line: " + line);
        }
        String version = tokens[1];
        System.out.println("Parsed header. Version: " + version);
    }

    private void parseMetaData(String line, String delimiter) {
        // Implement parsing logic for the meta data section using the delimiter

    }

    private void parseGlobalData(String line, String delimiter) {
        // Implement parsing logic for the global section data using the delimiter
    }

    private void parseRecordHeader(String line, BufferedReader reader, String delimiter) throws IOException {
        // Implement parsing logic for the record header using the delimiter
    }

    private void parseBinData(String line, String delimiter) {
        String[] tokens = line.split(delimiter);
        if (tokens.length < 3 || !tokens[0].equals("-")) {
            throw new IllegalArgumentException("Invalid bin data line: " + line);
        }
        String binType = tokens[1];
        String binName = tokens[2];
        String binValue = tokens.length > 3 ? tokens[3] : null;

        switch (binType) {
            case "N":
                System.out.println("Parsed NIL bin data. Bin Name: " + binName);
                break;
            case "Z":
                System.out.println("Parsed Boolean bin data. Bin Name: " + binName + ", Value: " + binValue);
                break;
            case "I":
                System.out.println("Parsed Integer bin data. Bin Name: " + binName + ", Value: " + binValue);
                break;
            case "D":
                System.out.println("Parsed Double bin data. Bin Name: " + binName + ", Value: " + binValue);
                break;
            case "S":
                System.out.println("Parsed String bin data. Bin Name: " + binName + ", Value: " + binValue);
                break;
            case "B":
                System.out.println("Parsed Bytes bin data. Bin Name: " + binName + ", Value: " + binValue);
                break;
            case "J":
                System.out.println("Parsed Java bin data. Bin Name: " + binName + ", Value: " + binValue);
                break;
            case "C":
                System.out.println("Parsed C# bin data. Bin Name: " + binName + ", Value: " + binValue);
                break;
            case "P":
                System.out.println("Parsed Python bin data. Bin Name: " + binName + ", Value: " + binValue);
                break;
            case "R":
                System.out.println("Parsed Ruby bin data. Bin Name: " + binName + ", Value: " + binValue);
                break;
            case "H":
                System.out.println("Parsed PHP bin data. Bin Name: " + binName + ", Value: " + binValue);
                break;
            case "E":
                System.out.println("Parsed Erlang bin data. Bin Name: " + binName + ", Value: " + binValue);
                break;
            case "Y":
                System.out.println("Parsed HyperLogLog bin data. Bin Name: " + binName + ", Value: " + binValue);
                break;
            case "M":
                System.out.println("Parsed Map bin data. Bin Name: " + binName + ", Value: " + binValue);
                break;
            case "L":
                System.out.println("Parsed List bin data. Bin Name: " + binName + ", Value: " + binValue);
                break;
        }
    }
}
