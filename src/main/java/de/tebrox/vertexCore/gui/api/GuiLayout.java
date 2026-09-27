package de.tebrox.vertexCore.gui.api;

import java.util.*;

public final class GuiLayout {
    private final int rows;
    private final List<Integer> contentSlots;
    private final Map<String, Integer> roleSlots;

    public GuiLayout(Builder builder) {
        this.rows = builder.rows;
        this.contentSlots = List.copyOf(builder.contentSlots);
        this.roleSlots = Collections.unmodifiableMap(new LinkedHashMap<>(builder.roleSlots));

        validate();
    }

    public static Builder builder() {
        return new Builder();
    }

    public int rows() {
        return rows;
    }

    public int size() {
        return rows * 9;
    }

    public List<Integer> contentSlots() {
        return contentSlots;
    }

    public int[] contentSlotsArray() {
        return contentSlots.stream().mapToInt(Integer::intValue).toArray();
    }

    public Map<String, Integer> roleSlots() {
        return roleSlots;
    }

    public OptionalInt slot(String role) {
        Objects.requireNonNull(role, "role");

        Integer slot = roleSlots.get(role);
        return slot == null ? OptionalInt.empty() : OptionalInt.of(slot);
    }

    public int requireSlot(String role) {
        return slot(role).orElseThrow(() -> new IllegalStateException("Required GUI layout role is missing: " + role));
    }

    public boolean hasRole(String role) {
        Objects.requireNonNull(role, "role");
        return roleSlots.containsKey(role);
    }

    private void validate() {
        int size = size();

        Set<Integer> used = new HashSet<>();

        for(int slot : contentSlots) {
            validateSlot("content", slot, size);
            if(!used.add(slot)) throw new IllegalStateException("Duplicate GUI layout slot: " + slot);
        }

        for(Map.Entry<String, Integer> entry : roleSlots.entrySet()) {
            int slot = entry.getValue();
            validateSlot("role '" + entry.getKey() + "'", slot, size);
            if(!used.add(slot)) throw new IllegalStateException("Gui layout role '" + entry.getKey() + "' conflicts with slot " + slot);
        }
    }

    private static void validateSlot(String source, int slot, int size){
        if(slot < 0 || slot >= size) throw new IllegalStateException("GUI layout " + source + " slot " + slot + " is outside inventory size " + size);
    }

    public static final class Builder {
        private int rows = 3;
        private final List<Integer> contentSlots = new ArrayList<>();
        private final Map<String, Integer> roleSlots = new LinkedHashMap<>();

        private Builder() {}

        public Builder rows(int rows) {
            if(rows < 1 || rows > 6) throw new IllegalArgumentException("GUI rows must be between 1 and 6");
            this.rows = rows;

            return this;
        }

        public Builder contentSlots(int... slots) {
            Objects.requireNonNull(slots, "slots");
            contentSlots.clear();

            for(int slot : slots) {
                if(slot < 0) throw new IllegalArgumentException("Content slot must not be negative");
                contentSlots.add(slot);
            }

            return this;
        }

        public Builder role(String role, int slot) {
            Objects.requireNonNull(role, "role");

            if(role.isBlank()) throw new IllegalArgumentException("GUI layout role must not be blank");
            if(slot < 0) throw new IllegalArgumentException("Role slot must not be negative");
            if(roleSlots.containsKey(role)) throw new IllegalArgumentException("Duplicate GUI layout role: " + role);

            roleSlots.put(role, slot);

            return this;
        }

        public GuiLayout build() {
            return new GuiLayout(this);
        }
    }
}
