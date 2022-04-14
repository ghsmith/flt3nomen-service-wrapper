package f3nsw;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;

@Path("variants")
public class Variants {
    
    @JsonIgnoreProperties(ignoreUnknown = true) 
    public static class Variant {
        @JsonProperty
        String nucleotideSequence;
        @JsonProperty
        Integer nucleotideLength;
        @JsonProperty
        Integer aaLength;
        @JsonProperty
        String hgvsp;
        @JsonProperty
        String hgvsc;
	@JsonProperty
	String hgvsg;
        @JsonProperty
        String nucleotideDupIns;
        @JsonProperty
        String aaDupIns;
	@JsonProperty
	String[] additionalInfo;
        @JsonProperty
        String status;
        public Variant() {
        }
    }

    @GET
    @Produces("application/json")
    public Variant getJson() throws IOException {

        return new Variant();

    }
    
    
    @GET
    @Produces("application/json")
    @Path("nucleotideSequence/{nucleotideSequence}")
    public Variant postJson(@PathParam("nucleotideSequence") String nucleotideSequence) throws IOException {

        Variant variant = new Variant();
        variant.nucleotideSequence = nucleotideSequence;
        
        HttpPost post = new HttpPost("https://flt3-itd-nomenclature.ue.r.appspot.com/");
        post.setEntity(
            new UrlEncodedFormEntity(
                Arrays.asList(new BasicNameValuePair[] { new BasicNameValuePair("result_seq", nucleotideSequence) })
            )
        );
        try(
            CloseableHttpClient httpClient = HttpClients.createDefault();
            CloseableHttpResponse response = httpClient.execute(post);
        ) {

            Pattern p = Pattern.compile(" *FLT3-ITD \\((.*) bp\\): (c\\..*) \\((p\\..*)\\)<br> *(chr13.*)<br><br> *Additional information:<br>.*NT sequence \\(.* bp\\): \\[(.*)]<br>.*AA sequence \\((.*) AA\\): \\[(.*)]<br><br>(.*)");

            BufferedReader htmlStream = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));

            String htmlLine;
            while((htmlLine = htmlStream.readLine()) != null) {

                if(htmlLine.equals("<h1>We have a problem!</h1>")) {

                    variant.status = "ERROR";

                    Pattern pErrorDetail1 = Pattern.compile("<h3>(.*)$");

                    while((htmlLine = htmlStream.readLine()) != null) {

                        Matcher mErrorDetail = pErrorDetail1.matcher(htmlLine);
                        if(mErrorDetail.matches()) {
                            variant.status = mErrorDetail.group(1);
                            break;
                        }
                    }

                    break;

                }

                Matcher m = p.matcher(htmlLine);
                if(m.matches()) {
                    variant.nucleotideLength = Integer.valueOf(m.group(1));
                    variant.aaLength = Integer.valueOf(m.group(6));
                    variant.hgvsp = m.group(3);
                    variant.hgvsc = m.group(2);
                    variant.hgvsg = m.group(4);
                    variant.nucleotideDupIns = m.group(5);
                    variant.aaDupIns = m.group(7);
                    variant.status = "OK";
		    variant.additionalInfo = m.group(8).split("<br>");
                    break;
                }

            }

        }

        return variant;

    }

}
