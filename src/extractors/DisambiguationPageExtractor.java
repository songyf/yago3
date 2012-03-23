package extractors;

import java.io.BufferedReader;
import java.io.File;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javatools.administrative.Announce;
import javatools.datatypes.FinalSet;
import javatools.filehandlers.FileLines;
import javatools.util.FileUtils;
import basics.Fact;
import basics.FactCollection;
import basics.FactSource;
import basics.FactWriter;
import basics.Theme;
import extractorUtils.FactTemplateExtractor;

/**
 * Extracts means facts from Wikipedia disambiguation pages
 * 
 * @author Johannes Hoffart
 * 
 */
public class DisambiguationPageExtractor extends Extractor {

	/** Input file */
	private File wikipedia;

	@Override
	public Set<Theme> input() {
		return new HashSet<Theme>(Arrays.asList(PatternHardExtractor.DISAMBIGUATIONTEMPLATES,
				PatternHardExtractor.TITLEPATTERNS, WordnetExtractor.WORDNETWORDS));
	}
	
  @Override
 public Set<Extractor> followUp() {
   return new HashSet<Extractor>(Arrays.asList(new RedirectExtractor(wikipedia, DIRTYDISAMBIGUATIONMEANSFACTS, REDIRECTEDDISAMBIGUATIONMEANSFACTS), new TypeChecker(
       REDIRECTEDDISAMBIGUATIONMEANSFACTS, DISAMBIGUATIONMEANSFACTS)));
 }

  /** Means facts from disambiguation pages */
  public static final Theme DIRTYDISAMBIGUATIONMEANSFACTS = new Theme("dirtyDisambiguationMeansFacts",
      "Means facts from disambiguation pages - needs redirecting and typechecking");
  
  /** Means facts from disambiguation pages */
  public static final Theme REDIRECTEDDISAMBIGUATIONMEANSFACTS = new Theme("redirectedDisambiguationMeansFacts",
      "Means facts from disambiguation pages - needs typechecking");
  
	/** Means facts from disambiguation pages */
	public static final Theme DISAMBIGUATIONMEANSFACTS = new Theme("disambiguationMeansFacts",
			"Means facts from disambiguation pages");

	@Override
	public Set<Theme> output() {
		return new FinalSet<Theme>(DIRTYDISAMBIGUATIONMEANSFACTS);
	}

	@Override
	public void extract(Map<Theme, FactWriter> output, Map<Theme, FactSource> input) throws Exception {
		// Extract the information
		Announce.doing("Extracting disambiguation means");

		BufferedReader in = FileUtils.getBufferedUTF8Reader(wikipedia);

		FactCollection disambiguationPatternCollection = new FactCollection(
				input.get(PatternHardExtractor.DISAMBIGUATIONTEMPLATES));
		FactTemplateExtractor disambiguationPatterns = new FactTemplateExtractor(disambiguationPatternCollection,
				"<_disambiguationPattern>");
		Set<String> templates = disambiguationTemplates(disambiguationPatternCollection);

		FactWriter out = output.get(DISAMBIGUATIONMEANSFACTS);

		String titleEntity = null;
		while (true) {
			switch (FileLines.findIgnoreCase(in, "<title>")) {
			case -1:
				Announce.done();
				in.close();
				return;
			case 0:
				titleEntity = FileLines.readToBoundary(in, "</title>");
				break;
			default:
				if (titleEntity == null)
					continue;

				String page = FileLines.readBetween(in, "<text", "</text>");
				if (isDisambiguationPage(page, templates)) {
					for (Fact fact : disambiguationPatterns.extract(page, titleEntity)) {
						if (fact != null)
							out.write(fact);
					}
				}
			}
		}
	}

	/** Returns the set of disambiguation templates */
	public static Set<String> disambiguationTemplates(FactCollection disambiguationTemplates) {
		return (disambiguationTemplates.asStringSet("<_yagoDisambiguationTemplate>"));
	}

	private boolean isDisambiguationPage(String page, Set<String> templates) {
		for (String templName : templates) {
			if (page.contains(templName) || page.contains(templName.toLowerCase())) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Needs Wikipedia as input
	 * 
	 * @param wikipedia
	 *            Wikipedia XML dump
	 */
	public DisambiguationPageExtractor(File wikipedia) {
		this.wikipedia = wikipedia;
	}

}