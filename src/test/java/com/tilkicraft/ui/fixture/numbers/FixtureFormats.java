package com.tilkicraft.ui.fixture.numbers;
public final class FixtureFormats {
    public interface Format {}
    public static Object blank() { return BlankFormat.INSTANCE; }
}
final class BlankFormat implements FixtureFormats.Format {
    static final BlankFormat INSTANCE=new BlankFormat();
    private BlankFormat() {}
}
