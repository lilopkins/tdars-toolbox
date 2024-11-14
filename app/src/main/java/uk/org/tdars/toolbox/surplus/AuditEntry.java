package uk.org.tdars.toolbox.surplus;

import java.io.Serializable;
import java.time.OffsetDateTime;

import lombok.Getter;

@Getter
public class AuditEntry implements Serializable {
    private OffsetDateTime moment;
    private String entry;

    public AuditEntry(String entry) {
        this.moment = OffsetDateTime.now();
        this.entry = entry;
    }
}
