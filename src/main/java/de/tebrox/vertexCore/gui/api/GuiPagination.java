package de.tebrox.vertexCore.gui.api;

import java.util.*;

public final class GuiPagination {
    private final List<Integer> contentSlots;
    private final List<GuiItem> content;

    private final Integer previousSlot;
    private final GuiItem previousItem;

    private final Integer nextSlot;
    private final GuiItem nextItem;

    public GuiPagination(Builder builder) {
        this.contentSlots = List.copyOf(builder.contentSlots);
        this.content = List.copyOf(builder.content);
        this.previousSlot = builder.previousSlot;
        this.previousItem = builder.previousItem;
        this.nextSlot = builder.nextSlot;
        this.nextItem = builder.nextItem;
    }

    public static Builder builder() {
        return new Builder();
    }

    public List<Integer> contentSlots() {
        return contentSlots;
    }

    public List<GuiItem> content() {
        return content;
    }

    public int pageCount() {
        if(content.isEmpty()) return 1;
        return (content.size() + contentSlots.size() - 1) / contentSlots.size();
    }

    public boolean isContentSlot(int slot) {
        return contentSlots.contains(slot);
    }

    public boolean isPreviousSlot(int slot) {
        return previousSlot != null && previousSlot == slot;
    }

    public boolean isNextSlot(int slot) {
        return nextSlot != null && nextSlot == slot;
    }

    public boolean isManagedSlot(int slot) {
        return isContentSlot(slot) || isPreviousSlot(slot) || isNextSlot(slot);
    }

    public Optional<GuiItem> item(int pageIndex, int slot) {
        int slotIndex = contentSlots.indexOf(slot);
        if(slotIndex < 0) return Optional.empty();

        int contentIndex = pageIndex * contentSlots.size() + slotIndex;
        if(contentIndex < 0 || contentIndex >= content.size()) return Optional.empty();

        return Optional.of(content.get(contentIndex));
    }

    public OptionalInt previousSlot() {
        return previousSlot == null ? OptionalInt.empty() : OptionalInt.of(previousSlot);
    }

    public Optional<GuiItem> previousItem() {
        return Optional.ofNullable(previousItem);
    }

    public OptionalInt nextSlot() {
        return nextSlot == null ? OptionalInt.empty() : OptionalInt.of(nextSlot);
    }

    public Optional<GuiItem> nextItem() {
        return Optional.ofNullable(nextItem);
    }

    public Set<Integer> managedSlots() {
        Set<Integer> slots = new LinkedHashSet<>(contentSlots);
        if(previousSlot != null) slots.add(previousSlot);
        if(nextSlot != null) slots.add(nextSlot);

        return Collections.unmodifiableSet(slots);
    }

    public boolean hasPrevious(int pageIndex) {
        return pageIndex > 0;
    }

    public boolean hasNext(int pageIndex) {
        return pageIndex + 1 < pageCount();
    }

    void validateForSize(int size) {
        for(int slot : contentSlots) validateSlot(slot, size);

        if(previousSlot != null) validateSlot(previousSlot, size);
        if(nextSlot != null) validateSlot(nextSlot, size);
    }

    private static void validateSlot(int slot, int size) {
        if(slot < 0 || slot >= size) throw new IllegalStateException("Pagination slot " + slot + " is outside inventory size " + size);
    }

    public static final class Builder {
        private final List<Integer> contentSlots = new ArrayList<>();
        private final List<GuiItem> content = new ArrayList<>();

        private Integer previousSlot;
        private GuiItem previousItem;

        private Integer nextSlot;
        private GuiItem nextItem;

        private Builder() {}

        public Builder contentSlots(int...slots) {
            Objects.requireNonNull(slots, "slots");
            contentSlots.clear();

            Set<Integer> unique = new LinkedHashSet<>();
            for(int slot : slots) {
                if(slot < 0) throw new IllegalArgumentException("Content slot must not be negative");
                if(!unique.add(slot)) throw new IllegalArgumentException("Duplicate content slot: " + slot);
            }

            contentSlots.addAll(unique);
            return this;
        }

        public Builder content(Collection<GuiItem> content) {
            Objects.requireNonNull(content, "content");
            this.content.clear();
            for(GuiItem item : content) {
                this.content.add(Objects.requireNonNull(item, "content item"));
            }

            return this;
        }

        public Builder previous(int slot, GuiItem item) {
            if(slot < 0) throw new IllegalArgumentException("Previous slot must not be negative");

            this.previousSlot = slot;
            this.previousItem = Objects.requireNonNull(item, "item");

            return this;
        }

        public Builder next(int slot, GuiItem item) {
            if(slot < 0) throw new IllegalArgumentException("Next slot must not be negative");

            this.nextSlot = slot;
            this.nextItem = Objects.requireNonNull(item, "item");

            return this;
        }

        public GuiPagination build() {
            if(contentSlots.isEmpty()) throw new IllegalStateException("Pagination requires at least one content slot");
            if(previousSlot != null && contentSlots.contains(previousSlot)) throw new IllegalStateException("Previous slot must not be a content slot");
            if(nextSlot != null && contentSlots.contains(nextSlot)) throw new IllegalStateException("Next slot must not be a content slot");
            if(previousSlot != null && previousSlot.equals(nextSlot)) throw new IllegalStateException("Previous and next slot must be different");

            return new GuiPagination(this);
        }
    }
}
