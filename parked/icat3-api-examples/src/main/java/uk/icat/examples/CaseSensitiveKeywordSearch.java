/**
 * Example to perform case sensitive keyword search on investigations
 * using ICAT API.
 * 
 * @author Richard Tyer
 * 
 * $Date: 2011-08-08 17:23:28 +0100 (Mon, 08 Aug 2011) $
 * $Revision: 932 $
 * $LastChangedBy: abm65@FED.CCLRC.AC.UK $
 *
 * $Id: CaseSensitiveKeywordSearch.java 932 2011-08-08 16:23:28Z abm65@FED.CCLRC.AC.UK $
 */
package uk.icat.examples;
 
import uk.icat3.client.ICAT;
import uk.icat3.client.Investigation;
import uk.icat3.client.InvestigationInclude;
import uk.icat3.client.KeywordDetails;

public class CaseSensitiveKeywordSearch extends ExampleBase {

	public static void main(String[] args) throws Exception {

		ICAT icat = getIcat();

		String sid = icat.login(username, password);

		KeywordDetails kd = new KeywordDetails();
		kd.setKeywords(keywordsList.toArray(new String[0]));

		kd.setCaseSensitive(false);
		kd.setInvestigationInclude(InvestigationInclude.KEYWORDS_ONLY);
		Investigation[] investigations = icat.searchByKeywordsAll(sid, kd, 0, 200);

		for (Investigation inv : investigations) {
			long id = inv.getId();
			String title = inv.getTitle();
			System.out.println("Investigation - ID: " + id + "\tTitle: " + title);

			if (inv.getKeywordCollection().length > 0) {
				System.out.println("Investigation has keywords!");
			}
		}
	}
}
