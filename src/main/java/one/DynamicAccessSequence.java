package one;

import edu.util.ApplicationClassFilter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static one.Constants.*;

public class DynamicAccessSequence {
    List<String> accessSequence;

    public DynamicAccessSequence(String line){
        this.accessSequence = new ArrayList<>(Arrays.asList(line.split(DELIMITER)));
        this.accessSequence.removeIf(NOT_TARGET_PACKAGE);
    }
}
