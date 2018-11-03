package lab1.data.frame;

import lab3.DateTimeValue;
import lab3.StringValue;
import lab3.Value;
import lab4.Applyable;
import lab4.GroupBy;
import lab4.Operation;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;


public class DataFrame {

    private List<Column> columns;

    /**
     * Constructs DataFrame with empty Columns
     *
     * @param names names of Columns
     * @param clazz types for Columns to hold
     */
    public DataFrame(String[] names, Class<? extends Value>[] clazz) {
        columns = new ArrayList<>();
        for (int i = 0; i < clazz.length; i++) {
            if (names.length <= i) {
                break;
            }

            if (isUnique(names[i])) {
                columns.add(new Column(names[i], clazz[i]));
            }
        }
    }

    /**
     * Create empty DataFrame
     */
    public DataFrame() {
        columns = new ArrayList<>();
    }

    /**
     * Constructor DataFrame wchic reads data from csv file,
     * if not double or integer types, arguments must be String(Char and Byte too)
     *
     * @param file    CSV file
     * @param classes type
     * @throws IOException
     */
    public DataFrame(String file, Class<? extends Value>[] classes) throws IOException, NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {

        FileInputStream fstream = new FileInputStream(file);
        BufferedReader br = new BufferedReader(new InputStreamReader(fstream));

        String[] columnNames = br.readLine().split(",");
        columns = new ArrayList<>();

        for (int i = 0; i < classes.length; i++) {
            columns.add(new Column(columnNames[i], classes[i]));
        }

        String strLine;
        Value[] values = new Value[columns.size()];
        List<Constructor<? extends Value>> constructors = new ArrayList<>(classes.length);
        for (int i = 0; i < classes.length; i++) {
            constructors.add(classes[i].getConstructor(String.class));
        }

        while ((strLine = br.readLine()) != null) {
            String[] str = strLine.split(",");
            for (int i = 0; i < str.length; i++) {
                values[i] = constructors.get(i).newInstance(str[i]);
            }
            addRow(values.clone());
        }

        br.close();
    }

    /**
     * Constructor with ArrayList of columns as parameter
     *
     * @param columns list of columns
     */
    public DataFrame(List<Column> columns) {
        this.columns = columns;
    }

    protected boolean isUnique(String name) {
        return columns.stream()
                .noneMatch(c -> c.getName().equals(name));
    }

    /**
     * Add row to DataFrame
     *
     * @param values Objects to add to DataFrame
     * @return true if amount of passed objects match to amount of Columns and Objects to each Column have same type
     */
    public boolean addRow(Value... values) {
        if (columns.size() != values.length) { ;
            return false;
        }

        IntStream.range(0, columns.size())
                .forEach(i -> columns.get(i).addElement(values[i]));
        return true;
    }


    /**
     * Used only for inner class
     *
     * @param values
     * @return
     */
    private boolean addRow(List<Value> values) {
        IntStream.range(0, columns.size())
                .forEach(i -> columns.get(i).addElement(values.get(i)));
        return true;
    }

    /**
     * Returns actual amount of rows in DataFrame(every Column has the same amount of rows)
     *
     * @return actual amount of rows
     */
    public int size() {
        return columns.isEmpty() ? 0 : columns.get(0).size();
    }

    /**
     * Returns String representation of DataFrame
     *
     * @return a string representation of DataFrame
     */
    @Override
    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        for (var s: columns) {
            stringBuilder.append(s.getName()).append("\t\t");
        }
        stringBuilder.append('\n');

        for (int i = 0; i < size(); i++) {
            for (var value: columns) {
                stringBuilder.append(value.getElement(i)).append('\t');
            }
            stringBuilder.append('\n');
        }
        return stringBuilder.toString();
    }

    /**
     * Returns the Column with specified name
     *
     * @param name Column name to get
     * @return Column with specified name
     */
    public Column getColumn(String name) {
        return columns.stream()
                .filter(c -> c.getName().equals(name))
                .findFirst()
                .orElse(null);
    }

    /**
     * Returns Data Frame with specified Columns by their names
     *
     * @param cols names of Columns to copy
     * @param copy true - deep copy or false to shallow copy
     * @return new DataFrame with Columns with specified name
     */
    public DataFrame get(String[] cols, boolean copy) {
        DataFrame output = new DataFrame();

        for (String s : cols) {
            for (Column c : columns) {
                if (s.equals(c.getName())) {
                    output.columns.add(copy ? c.clone() : c);
                    break;
                }
            }
        }
        return output;
    }

    /**
     * Return new DataFrame with one specified row by index
     *
     * @param i index of Row to copy
     * @return new DataFrame with one row
     */
    public DataFrame iloc(int i) {
        DataFrame output = new DataFrame(getColumnNames(), getClasses());

        int k = 0;
        if (i >= 0 && i < size()) {
            for (Column c : output.columns) {
                c.addElement(columns.get(k++).getElement(i));
            }
        }
        return output;
    }

    /**
     * Return new DataFrame with range of rows in this DataFrame
     *
     * @param from from which index to copy
     * @param to   to which index to copy
     * @return new DataFrame with specified rows
     */
    public DataFrame iloc(int from, int to) {
        DataFrame output = new DataFrame();
        from = (from < 0) ? 0 : from;

        for (Column c : columns) {
            Column column = new Column(c.getName(), c.getClazz());
            for (int i = from; (i <= to) && (i < size()); i++) {
                column.addElement(c.getElement(i));
            }
            output.columns.add(column);
        }

        return output;
    }

    /**
     * Return array of Column names
     *
     * @return array of strings
     */
    public String[] getColumnNames() {
        String[] str = new String[columns.size()];
        Arrays.setAll(str, i -> columns.get(i).getName());
        return str;
    }

    public Value[] getRow(int index) {
        return columns.stream()
                      .map(column -> column.getElement(index))
                      .toArray(Value[]::new);
    }

    /**
     * Return array of Column types
     *
     * @return array of strings
     */
    public Class<? extends Value>[] getClasses() {
        Class[] classes = new Class[columns.size()];
        Arrays.setAll(classes, i ->  columns.get(i).getClazz());
        return classes;
    }

    /**
     * Return inner Class with sorted DataFrames
     * by values in Column with specified names
     *
     * @param colname Column names, by which DataFrame is sorted
     * @return Inner DataFrame
     */
    public DataFrameGroupBy groupBy(String... colname) {
        HashMap<List<Value>, DataFrame> map = new HashMap<>(colname.length);
        List<Column> columns = Arrays.stream(colname)
                                     .map(this::getColumn)
                                     .collect(Collectors.toList());

        for (int i = 0; i < size(); i++) {
            List<Value> values = new ArrayList<>(columns.size());

            for (var column: columns) {
                values.add(column.getElement(i));
            }

            if(!map.containsKey(values)) {
                map.put(values, iloc(i));
            } else {
                map.get(values).addRow(getRow(i));
            }
        }

        return new DataFrameGroupBy(map,colname);
    }

    public class DataFrameGroupBy implements GroupBy {

        private HashMap<List<Value>, DataFrame> map;
        private List<String> colNames;

        public DataFrameGroupBy(HashMap<List<Value>, DataFrame> map, String[] colNames) {
            this.map = map;
            this.colNames = Arrays.asList(colNames);
        }

        private DataFrame operation(Operation operation, boolean toDrop) {
            DataFrame dataFrame;

            // block of code to remove columns, which can't perform certain calculations
            if (toDrop) {
                List<Class<? extends Value>> classList = new ArrayList<>(List.of(getClasses()));
                ArrayList<String> nameList = new ArrayList<>(List.of(getColumnNames()));
                List<Integer> namesToRemove = new ArrayList<>();

                for (int i = 0; i < classList.size(); i++) {
                    if((classList.get(i).equals(StringValue.class) || classList.get(i).equals(DateTimeValue.class)) && !colNames.contains(nameList.get(i))) {
                        namesToRemove.add(i);
                    }
                }

                for (int i = namesToRemove.size() - 1; i >= 0; i--) {
                    nameList.remove((int)namesToRemove.get(i));
                    classList.remove((int)namesToRemove.get(i));
                }

                String[] names = nameList.toArray(String[]::new);
                Class[] classes = classList.toArray(Class[]::new);

                dataFrame = new DataFrame(names, classes);
            } else {
                dataFrame = new DataFrame(getColumnNames(), getClasses());
            }

            for (var values: map.keySet()) {
                List<Value> toAdd = new ArrayList<>(values);
                DataFrame df = map.get(values);

                for (var column: df.columns) {
                    if(!colNames.contains(column.getName())) {
                        if(toDrop && !(column.getClazz().equals(DateTimeValue.class) || column.getClazz().equals(StringValue.class))) {
                            toAdd.add(column.calculate(operation));
                        } else if(!toDrop) {
                            toAdd.add(column.calculate(operation));
                        }
                    }
                }
                dataFrame.addRow(toAdd);
            }
            return dataFrame;
        }

        @Override
        public DataFrame max() {
            return operation(Operation.MAX, false);
        }

        @Override
        public DataFrame min() {
            return operation(Operation.MIN, false);
        }

        @Override
        public DataFrame mean() {
            return operation(Operation.MEAN, true);
        }

        @Override
        public DataFrame std() {
            return operation(Operation.STD, true);
        }

        @Override
        public DataFrame sum() {
            return operation(Operation.SUM, true);
        }

        @Override
        public DataFrame var() {
            return operation(Operation.VAR, true);
        }

        @Override
        public DataFrame apply(Applyable applyable) {
            return applyable.apply(DataFrame.this);
        }
    }
}