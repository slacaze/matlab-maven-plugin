package fx.maven;

import com.opencsv.bean.CsvBindByName;

public class TestResult {
    @CsvBindByName
    public String Name;
    @CsvBindByName
    public boolean Passed;
    @CsvBindByName
    public boolean Failed;
    @CsvBindByName
    public boolean Incomplete;
    @CsvBindByName
    public double Duration;
    @CsvBindByName
    public String Details;
}
