package net.shyue.smurf.Utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author shyue
 */
public class SimpleCSVParser {

    private List<String[]> data;
    private int colNum;

    public SimpleCSVParser(InputStream is) throws IOException {
        BufferedReader csvBufReader = new BufferedReader(new InputStreamReader(is));
        data = new ArrayList<String[]>();
        String line;
        while ((line = csvBufReader.readLine()) != null) {
            String [] tokStr = line.split(",");
            for (int i =0; i<tokStr.length;i++)
            {
                tokStr[i] = tokStr[i].replace("\"", "");
                tokStr[i] = tokStr[i].trim();
            }
            data.add(tokStr);
            colNum = tokStr.length;
        }
        csvBufReader.close();

    }

    public String[] getRow(int index)
    {
        if (index < data.size())
        {
            return data.get(index);
        }
        else
        {
            throw new IndexOutOfBoundsException("Index outside of number of rows.");
        }
    }

    public int getColumnsNum() {
        return colNum;
    }

    public List<String[]> getData() {
        return data;
    }

}