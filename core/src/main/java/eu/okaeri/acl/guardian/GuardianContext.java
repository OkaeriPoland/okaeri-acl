package eu.okaeri.acl.guardian;

import lombok.*;

import java.util.LinkedHashMap;
import java.util.Map;

@Getter
@EqualsAndHashCode
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
public class GuardianContext {

    private Map<String, Object> data = new LinkedHashMap<>();

    public static GuardianContext of() {
        return new GuardianContext();
    }

    public static GuardianContext of(Map<String, Object> data) {
        return new GuardianContext(data);
    }

    public GuardianContext with(@NonNull String key, Object value) {
        this.data.put(key, value);
        return this;
    }

    @Override
    public String toString() {
        return "GuardianContext(data=" + data.keySet() + ')';
    }
}
