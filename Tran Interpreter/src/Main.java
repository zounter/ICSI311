import AST.TranNode;
import Interpreter.Interpreter;
import Tran.Lexer;
import Tran.Parser;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Scanner;

public class Main {

    public static void main(String [] args)
    {
        Main m = new Main();
        m.runFile();
    }

    public void runFile() {
        Scanner input = new Scanner(System.in);
        while (true) {
            System.out.print("Choose a tran file to run:");
            String fileName = input.next();
            String tranCode = "";

            FileInputStream getFile = null;
            try {
                getFile = new FileInputStream(fileName);
                byte[] temp = new byte[getFile.available()];
                temp = getFile.readAllBytes();
                tranCode = new String(temp, StandardCharsets.US_ASCII);
                Lexer l = new Lexer(tranCode);
                var Tran = new TranNode();
                var Tokens = l.Lex();
                Parser p = new Parser(Tran, Tokens);
                p.Tran();
                var i = new Interpreter(Tran);
                i.start();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
                System.out.println();
            } catch (Exception e) {
                e.printStackTrace();
                System.out.println();
            }
        }
    }

}
