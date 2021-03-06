package br.com.caelum.tubaina.parser.html.kindle;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import br.com.caelum.tubaina.BookPart;
import br.com.caelum.tubaina.Chapter;
import br.com.caelum.tubaina.SectionsManager;
import br.com.caelum.tubaina.TubainaBuilder;
import br.com.caelum.tubaina.builder.BookPartsBuilder;
import br.com.caelum.tubaina.builder.ChapterBuilder;
import br.com.caelum.tubaina.io.KindleResourceManipulatorFactory;
import br.com.caelum.tubaina.io.TubainaHtmlDir;
import br.com.caelum.tubaina.parser.Parser;
import br.com.caelum.tubaina.parser.RegexConfigurator;
import br.com.caelum.tubaina.parser.html.desktop.HtmlParser;
import freemarker.ext.beans.BeansWrapper;
import freemarker.template.Configuration;

public class PartToKindleTest {

    private PartToKindle partToKindle;

    @Before
    public void setUp() throws IOException {
        Configuration cfg = new Configuration();
        cfg.setDirectoryForTemplateLoading(new File(TubainaBuilder.DEFAULT_TEMPLATE_DIR, "kindle"));
        cfg.setObjectWrapper(new BeansWrapper());

        Parser parser = new HtmlParser(new RegexConfigurator().read("/regex.properties",
                "/kindle.properties"));

        partToKindle = new PartToKindle(parser, cfg, new ArrayList<String>());
    }

    private Chapter createChapter(String title, String introduction, String content) {
        return new ChapterBuilder(title, introduction, content, 1, new SectionsManager()).build();
    }

    private int countOccurrences(String text, String substring) {
        String[] tokens = text.split(substring);
        return tokens.length - 1;
    }

    @Test
    public void testGeneratePartWithChapters() {
        Chapter chapter = createChapter("chapter title", "introduction",
                "[section section one] section content");
        List<BookPart> bookParts = new BookPartsBuilder(new SectionsManager()).addPartFrom("[part \"parte 1\"]")
                .addChaptersToLastAddedPart(Arrays.asList(chapter)).build();
        BookPart part = bookParts.get(0);
        new KindleModule().inject(part);
        
		String generatedContent = partToKindle.generateKindlePart(part,
                new TubainaHtmlDir(null, null, new KindleResourceManipulatorFactory()), 1)
                .toString();
        assertEquals(1, countOccurrences(generatedContent, "<h1>Part 1 - parte 1</h1>"));
        assertEquals(1, countOccurrences(generatedContent, "<h2.*>\\d+ - chapter title</h2>"));
        assertEquals(1, countOccurrences(generatedContent,
                "<h3.*>\\W*\\d+\\.1 - section one\\W*</h3>"));
    }

    @Test
    public void testGenerateANotPrintablePartWithChapters() {
        Chapter chapter = createChapter("chapter title", "introduction",
                "[section section one] section content");
        List<BookPart> bookParts = new BookPartsBuilder(new SectionsManager()).addChaptersToLastAddedPart(
                Arrays.asList(chapter)).build();
        BookPart part = bookParts.get(0);
        new KindleModule().inject(part);
		String generatedContent = partToKindle.generateKindlePart(part,
                new TubainaHtmlDir(null, null, new KindleResourceManipulatorFactory()), 1).toString();
        assertEquals(0, countOccurrences(generatedContent, "<h1>.*</h1>"));
        assertEquals(1, countOccurrences(generatedContent, "<h2.*>\\d+ - chapter title</h2>"));
        assertEquals(1, countOccurrences(generatedContent,
                "<h3.*>\\W*\\d+\\.1 - section one\\W*</h3>"));
    }
}
