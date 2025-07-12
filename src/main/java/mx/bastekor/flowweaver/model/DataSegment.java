package mx.bastekor.flowweaver.model;

import java.util.List;

// Está revisar porque no me llama la atencion
public class DataSegment extends LogSegment {
    private List<String> values; // Solo valores, no claves

    @Override
    public String toPipeString() {
        return String.join("|", values);
    }
}
