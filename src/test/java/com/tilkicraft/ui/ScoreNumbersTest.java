package com.tilkicraft.ui;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class ScoreNumbersTest {
    static class CraftObjectiveFixture { final Object target=new Object(); Object getHandle() { return target; } }
    static class InheritedFixture extends CraftObjectiveFixture { }
    public static class NmsHandle {
        Object format;
        public void setNumberFormat(com.tilkicraft.ui.fixture.numbers.FixtureFormats.Format value) { format=value; }
    }
    static class SpigotObjectiveFixture { final NmsHandle target=new NmsHandle(); NmsHandle getHandle() { return target; } }
    @Test void canReadTheNonPublicCraftHandleThatThePreviousImplementationMissed() throws Exception {
        var fixture=new InheritedFixture();assertSame(fixture.target,ScoreNumbers.handle(fixture));
    }
    @Test void unsupportedServersReportFailureInsteadOfPretendingScoresWereHidden() { assertFalse(ScoreNumbers.hide(new Object())); }
    @Test void setsBlankFormatThroughAHiddenCraftHandleAndPackagePrivateSingleton() {
        var fixture=new SpigotObjectiveFixture();assertTrue(ScoreNumbers.hide(fixture));
        assertSame(com.tilkicraft.ui.fixture.numbers.FixtureFormats.blank(),fixture.target.format);
    }
}
