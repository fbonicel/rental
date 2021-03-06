package jobs;

import java.util.*;
import java.collections.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import notifiers.Mails;

import models.Rental;
import play.Invoker;
import play.Logger;
import play.Play;
import play.Invoker.Invocation;
import play.jobs.Every;
import play.jobs.*;
import play.libs.WS;

import org.jsoup.*;
import org.jsoup.nodes.*;
import org.jsoup.select.*;


@Every("5mn")
public class LBCRentalSearcher extends Job{
private static List<String> locations = new ArrayList<String>(
	Arrays.asList("Paris%2075020", "Paris%2075019", "Paris%2075018", "Montreuil%2075010"));
public void doJob() throws Exception {
		Logger.info("Searching for new rentals on LeBonCoin...");
		List<Rental> found = suggestLeBonCoinRentals();
		Logger.info("Have found %d rental(s) on LEBONCOIN", found.size());
		if (!found.isEmpty()) Mails.newRentals(found);
}
private static Pattern lbcRentalId = Pattern.compile("locations/(\\d*)");

public static List<Rental> suggestLeBonCoinRentals() throws Exception{
	List<Rental> found = new ArrayList<Rental>();
	for (String location : locations){
		String lbcPage = WS.url("http://www.leboncoin.fr/locations/offres/ile_de_france/paris/?f=a&th=1&mrs=600&mre=900&sqs=3&ros=2&roe=2&ret=1&ret=2&furn=2&location="+location).get().getString();
		Document doc = Jsoup.parse(lbcPage);
		Elements rentalLinks = doc.select(".list-lbc a");

		for (Element link : rentalLinks) {
		  String linkHref = link.attr("href");
		  String linkdescription = link.attr("title");
		  String lbcRental = WS.url(linkHref).get().getString();
		  Document rentalDoc = Jsoup.parse(lbcRental);
		  String description = rentalDoc.select("div .AdviewContent .content").html();
		  String image = rentalDoc.select("div .images_cadre a").attr("style");
		  if (image.indexOf("'") > 0) {
		  	Logger.debug(image.split("'")[1]);
		  	description +="<a href=\""+linkHref+"\" target=\"_blank\"><img src=\""+image.split("'")[1]+"\"></a>\n";
		  }
		  Rental rental = new Rental();
		  rental.type="LBC";
		  rental.suggested = true;
		  Matcher matcher = lbcRentalId.matcher(linkHref);
		  if (matcher.find()) rental.externalId = matcher.group(1);
		  rental.name = "LBC -- "+rental.externalId;
		  if (!Rental.isExist(rental)){
		  	rental.text = linkdescription + "</br>"+ description;
		  	rental.save();
		  	found.add(rental);
		  } else {
	    	Logger.info("Rental {%s} already exists!", rental.externalId);
	      }
		
		  
		}
	}
	return found;
}
}