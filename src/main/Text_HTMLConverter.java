package org.jenkinsci.plugins.clairdockerscannerbuildstep;

import hudson.Launcher;
import hudson.EnvVars;
import hudson.Launcher.ProcStarter;
import hudson.FilePath;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.util.ArgumentListBuilder;

//import org.apache.http.HttpResponse;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.PrintStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.InterruptedIOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.Scanner;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.ServletException;

//import org.apache.http.HttpResponse;

import hudson.model.Computer;
import hudson.remoting.Callable;
import hudson.remoting.Channel;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.slaves.Channels;

import java.net.*;

/**
 * This class does the actual execution..
 * 
 */
public class Text_HTMLConverter {
	static int countHigh = 0;
	static int countMed = 0;
	static int countLow = 0;
	static int countNeg = 0;
	static int total;
	static int perHigh;
	static int perMed;
	static int perLow;
	static int perNeg;

	@SuppressWarnings("deprecation")
	public static FilePath text_to_html(File outFile, FilePath outfilFilePath1, PrintStream out, String build_no,
			FilePath target, AbstractBuild build, String jenkins_home, BuildListener listener, int buildNo,
			File historyoutFile, String localImage) throws IOException, InterruptedException {

		File file = new File(outFile.toString());

		EnvVars env = build.getEnvironment(listener);
		String JobName = env.get("JOB_NAME");

		String content = new Scanner(new File(outFile.toString())).useDelimiter("\\Z").next();
		System.out.println("Build no is in TMTC " + build_no);
		FileReader fileReader = new FileReader(file);
		BufferedReader bufferedReader = new BufferedReader(fileReader);

		String line = null;
		System.setProperty("hudson.model.DirectoryBrowserSupport.CSP", "");

		List<String> list = new ArrayList<String>();
		list.removeAll(Arrays.asList("", null));

		while ((line = bufferedReader.readLine()) != null) {
			list.add(line);
		}

		String[] stringArr = list.toArray(new String[0]);

		for (int j = 0; j < stringArr.length; j++) {
			stringArr[j] = stringArr[j].trim();
		}

		fileReader.close();

		String[] value = stringArr[0].split("Clair report for image ");
		String[] value1 = value[1].split(" ");
		// History file processing to get command in layer coloumn

		FileReader HistoryfileReader = new FileReader(historyoutFile);
		BufferedReader HistorybufferedReader = new BufferedReader(HistoryfileReader);
		FileInputStream fstream = new FileInputStream(historyoutFile);
		BufferedReader br = new BufferedReader(new InputStreamReader(fstream));
		String strLine;
		List<String> size = new ArrayList<String>();
		int index;
		String History_out = "";
		int lastindex;
		List<String> command = new ArrayList<String>();
		List<String> part1 = new ArrayList<String>();
		List<String> sizeoflayer = new ArrayList<String>();
		List<String> sizelayerwithoutmb = new ArrayList<String>();
		List<Integer> sizeoflayer_digits = new ArrayList<Integer>();
		List<String> size_layer_file = new ArrayList<String>();

		List<String> id = new ArrayList<String>();
		double sizeOfimage = 0;
		// StringBuilder str1 = new StringBuilder();
		while ((strLine = br.readLine()) != null) {
			// System.out.println(strLine);
			if (strLine.contains("/bin/sh -c")) {
				StringBuilder str = new StringBuilder();
				str.append(strLine);
				String arr[] = strLine.split(" "); // for extracting id
				id.add(arr[0]);
				// System.out.println(str);
				index = str.indexOf("-c ");
				History_out = str.substring(index + 2);
				// System.out.println(out);
				String[] part = History_out.split(" ");

				for (int i = 0; i < part.length; i++) {
					part1.add(part[i]);

				}
				List<String> output1 = new ArrayList<String>();
				for (int i = 0; i < part1.size(); i++) {
					if (!part1.get(i).isEmpty()) {
						output1.add(part1.get(i));
					}
				}
				// System.out.println(output1);
				StringBuilder str1 = new StringBuilder();
				for (int i = 0; i < output1.size() - 2; i++) {

					str1.append(output1.get(i) + " ");
				}
				command.add(str1.toString()); // to extract command

				lastindex = part1.size();
				StringBuilder str2 = new StringBuilder();
				str2.append(part1.get(lastindex - 2));
				str2.append(part1.get(lastindex - 1));
				// System.out.println("Str2 value"+str1);

				size.add(str2.toString());
				sizeoflayer.add(part1.get(lastindex - 2) + part1.get(lastindex - 1));

				sizelayerwithoutmb.add(part1.get(lastindex - 2)); // layers
				part1.clear();
			}

		}
		// System.out.println("withoud mb " + sizelayerwithoutmb);
		List<String> finalcommand = new ArrayList<String>();
		List<String> finalcommand1 = new ArrayList<String>();
		for (int i = 0; i < command.size(); i++) {
			// System.out.println(command.get(i));
			if (command.get(i).contains("(nop)")) {
				finalcommand.add(command.get(i).replace("#(nop) ", " "));

			}

			else
				finalcommand.add(command.get(i));
		}
		for (int j = 0; j < finalcommand.size(); j++) {

			finalcommand1.add(finalcommand.get(j).replace("\'", ""));

		}
		double total_image_size = 0;
		for (int i = 0; i < sizeoflayer.size(); i++) {
			if (sizeoflayer.get(i).contains("kB")) {
				total_image_size = total_image_size + (Double.parseDouble(sizelayerwithoutmb.get(i)) / 1000);
				size_layer_file.add(String.valueOf(((Double.parseDouble(sizelayerwithoutmb.get(i)) / 1000))));
			} else if (sizeoflayer.get(i).contains("MB")) {
				total_image_size = total_image_size + (Double.parseDouble(sizelayerwithoutmb.get(i)));
				size_layer_file.add(String.valueOf(((Double.parseDouble(sizelayerwithoutmb.get(i))))));
			} else {
				total_image_size = (total_image_size + (Double.parseDouble(sizelayerwithoutmb.get(i)) / 1000 / 1000));
				size_layer_file.add(String.valueOf(((Double.parseDouble(sizelayerwithoutmb.get(i)) / 1000 / 1000))));

			}

		}
		System.out.println("total image size" + total_image_size);
		System.out.println("id list : " + id);
		System.out.println("command list : " + command);
		System.out.println("Size of each layer : " + sizeoflayer);
		System.out.println("size of layers in mb is " + size_layer_file);
		System.out.println("final command new one:" + finalcommand1);
		// data of line chart
		// File file1 = Values_File.valuesFile(countHigh, countMed, countLow,
		// countNeg, build_no, build, jenkins_home,
		// listener);

		// data of dropdown graph
		File layer_file = Values_File.layervalues(finalcommand1, size_layer_file, build_no, build, jenkins_home,
				listener);// calls Values_file.java to create the prop file
		// data of image size
		String Filename = JobName + "_ImageSize_data.properties";
		File writer = new File(build.getRootDir(), "print_stream");
		FilePath workspace = build.getWorkspace();
		FilePath his_target = new FilePath(workspace, Filename);
		FilePath outFile1 = new FilePath(writer);

		String FILENAME = jenkins_home + "/" + Filename;

		File file_his = new File(FILENAME);
		// System.out.println("Filename of properties file" + FILENAME);
		BufferedWriter bw = null;
		FileWriter fw = null;
		String data = null;

		try {

			data = (build_no + " = " + build_no + ", " + total_image_size + "\n");

			// if file doesnt exists, then create it
			if (!file_his.exists()) {
				file_his.createNewFile();
			}

			// true = append file
			fw = new FileWriter(file_his.getAbsoluteFile(), true);
			bw = new BufferedWriter(fw);

			bw.write(data);

			System.out.println("Done");

		} catch (IOException e) {

			e.printStackTrace();

		} finally {

			try {

				if (bw != null)
					bw.close();

				if (fw != null)
					fw.close();

			} catch (IOException ex) {

				ex.printStackTrace();

			}
		}

		FilePath Filepath1 = new FilePath(file_his);
		Filepath1.copyTo(his_target);
		// end image size data

		// fetching data from layer_file
		Properties props2 = new Properties();
		FileInputStream fis2 = new FileInputStream(layer_file);

		props2.load(fis2);
		Set<Object> layer_file_keys = props2.keySet();

		List<Object> list_layerfile_Keys = new ArrayList<Object>();
		list_layerfile_Keys.addAll(layer_file_keys);

		List<Integer> layer_file_keyList = new ArrayList<Integer>();
		for (Object object2 : list_layerfile_Keys) {
			String obj2 = object2.toString();
			Integer intobj2 = Integer.parseInt(obj2);
			layer_file_keyList.add(intobj2);
		}
		String layer_file_commands = null;
		String[] layer_command = null;

		List<String> layer_file_command = new ArrayList<String>();
		List<String> layer_file_size = new ArrayList<String>();

		Collections.sort(layer_file_keyList);

		System.out.println("build in layer_file	are" + layer_file_keyList.toString());
		// end history file data

		out.println("<!doctype html>");

		out.println(
				"<div id=Heading style=\" font-family: Helvetica, Arial, sans-serif;font-size: 11px;text-decoration:underline;width=100%;height=100%;\"><h2 align=\"center\" style=\"color:Black;font-size: 12px;font-weight: bold;\"><b>Docker Security Report - "
						+ value1[0] + "</b></h2>");
		out.println("</div");
		out.println("<html lang = \"en\">");
		out.println("<head>");
		out.println("<meta charset = \"utf-8\">");
		out.println("<title>Clair Inputs</title>");
		out.println("<link rel=\"stylesheet\" href=\"//code.jquery.com/ui/1.12.1/themes/base/jquery-ui.css\">");
		out.println("<link rel=\"stylesheet\" href=\"/resources/demos/style.css\">");
		out.println("<script src=\"https://code.jquery.com/jquery-1.12.4.js\"></script>");
		out.println("<script src=\"https://code.jquery.com/ui/1.12.1/jquery-ui.js\"></script>");
		out.println(
				"<link rel=\"stylesheet\" href=\"https://maxcdn.bootstrapcdn.com/bootstrap/3.3.7/css/bootstrap.min.css\">");
		out.println("<script src=\"https://ajax.googleapis.com/ajax/libs/jquery/3.2.1/jquery.min.js\"></script>");
		out.println("<script src=\"https://maxcdn.bootstrapcdn.com/bootstrap/3.3.7/js/bootstrap.min.js\"></script>");
		out.println("<link rel=\"stylesheet\" type=\"text/css\" href=\"styles/vanilla_mint/style.css\">");
		out.println("<script type=\"text/javascript\" src=\"app/scripts/si-object-mint.js\"></script>");
		out.println("<script type=\"text/javascript\" language=\"javascript\">");

		// <![CDATA[
		out.println("SI.Mint.collapse     = true;"
				+ "           window.onload = function() { SI.Mint.staggerPaneLoading(true); SI.Mint.sizePanes(); SI.Mint.onloadScrolls(); };"
				+ "           window.onresize      = function() { SI.Mint.sizePanes(); };" + "           </script>");
		out.println(
				"<link href = \"https://code.jquery.com/ui/1.10.4/themes/ui-lightness/jquery-ui.css\"\" rel = \"stylesheet\">");
		out.println("<script src = \"https://code.jquery.com/jquery-1.10.2.js\"></script>");
		out.println("<script src = \"https://code.jquery.com/ui/1.10.4/jquery-ui.js\"></script>");
		out.println();
		out.println("<script>");
		out.println("$(function() {");
		out.println("$( \"#tabs-1\" ).tabs();");
		out.println("});");
		out.println("</script>");
		out.println("<script type=\"text/javascript\">");
		out.println("  var tablesToExcel = (function() {");
		out.println("    var uri = 'data:application/vnd.ms-excel;base64,'");
		out.println(
				"    , tmplWorkbookXML = '<?xml version=\"1.0\"?><?mso-application progid=\"Excel.Sheet\"?><Workbook xmlns=\"urn:schemas-microsoft-com:office:spreadsheet\" xmlns:ss=\"urn:schemas-microsoft-com:office:spreadsheet\">'");
		out.println(
				"      + '<DocumentProperties xmlns=\"urn:schemas-microsoft-com:office:office\"><Author>Axel Richter</Author><Created>{created}</Created></DocumentProperties>'");
		out.println("      + '<Styles>'");
		out.println("      + '<Style ss:ID=\"Currency\"><NumberFormat ss:Format=\"Currency\"></NumberFormat></Style>'");
		out.println("      + '<Style ss:ID=\"Date\"><NumberFormat ss:Format=\"Medium Date\"></NumberFormat></Style>'");
		out.println("     + '</Styles>' ");
		out.println("     + '{worksheets}</Workbook>'");
		out.println("    , tmplWorksheetXML = '<Worksheet ss:Name=\"{nameWS}\"><Table>{rows}</Table></Worksheet>'");
		out.println(
				"    , tmplCellXML = '<Cell{attributeStyleID}{attributeFormula}><Data ss:Type=\"{nameType}\">{data}</Data></Cell>'");
		out.println("   , base64 = function(s) { return window.btoa(unescape(encodeURIComponent(s))) }");
		out.println("   , format = function(s, c) { return s.replace(/{(\\w+)}/g, function(m, p) { return c[p]; }) }");
		out.println("   return function(tables, wsnames, wbname, appname) {");
		out.println("     var ctx = \"\";");
		out.println("     var workbookXML = \"\";");
		out.println("     var worksheetsXML = \"\";");
		out.println("      var rowsXML = \"\";");
		out.println("      for (var i = 0; i < tables.length; i++) {");
		out.println("       if (!tables[i].nodeType) tables[i] = document.getElementById(tables[i]);");
		out.println("       for (var j = 0; j < tables[i].rows.length; j++) {");
		out.println("         rowsXML += '<Row>'");
		out.println("          for (var k = 0; k < tables[i].rows[j].cells.length; k++) {");
		out.println("            var dataType = tables[i].rows[j].cells[k].getAttribute(\"data-type\");");
		out.println("           var dataStyle = tables[i].rows[j].cells[k].getAttribute(\"data-style\");");
		out.println("          var dataValue = tables[i].rows[j].cells[k].getAttribute(\"data-value\");");
		out.println("           dataValue = (dataValue)?dataValue:tables[i].rows[j].cells[k].innerHTML;");
		out.println("           var dataFormula = tables[i].rows[j].cells[k].getAttribute(\"data-formula\");");
		out.println(
				"            dataFormula = (dataFormula)?dataFormula:(appname=='Calc' && dataType=='DateTime')?dataValue:null;");
		out.println(
				"            ctx = {  attributeStyleID: (dataStyle=='Currency' || dataStyle=='Date')?' ss:StyleID=\"'+dataStyle+'\"':''");
		out.println(
				"                  , nameType: (dataType=='Number' || dataType=='DateTime' || dataType=='Boolean' || dataType=='Error')?dataType:'String'");
		out.println("                   , data: (dataFormula)?'':dataValue");
		out.println("                  , attributeFormula: (dataFormula)?' ss:Formula=\"'+dataFormula+'\"':''");
		out.println("                 };");
		out.println("            rowsXML += format(tmplCellXML, ctx);");
		out.println("         }");
		out.println("         rowsXML += '</Row>'");
		out.println("        }");
		out.println("       ctx = {rows: rowsXML, nameWS: wsnames[i] || 'Sheet' + i};");
		out.println("       worksheetsXML += format(tmplWorksheetXML, ctx);");
		out.println("       rowsXML = \"\";");
		out.println("     }");
		out.println("      ctx = {created: (new Date()).getTime(), worksheets: worksheetsXML};");
		out.println("     workbookXML = format(tmplWorkbookXML, ctx);");
		out.println("console.log(workbookXML);");
		out.println("     var link = document.createElement(\"A\");");
		out.println("      link.href = uri + base64(workbookXML);");
		out.println("    link.download = wbname || 'Workbook.xls';");
		out.println("     link.target = '_blank';");
		out.println("      document.body.appendChild(link);");
		out.println("     link.click();");
		out.println("     document.body.removeChild(link);");
		out.println("   }");
		out.println("  })();");
		out.println("$( function() {			    $( \"#accordion\" ).accordion({"
				+ "			      collapsible: true," + "			            active: false,"
				+ "			            clearStyle: true" + "			    });" + "			  } );");
		out.println("  </script>");
		out.println("<style>");
		out.println("#tabs-1{font-size: 10px;font-weight:bold;}");
		out.println(".ui-widget-header {");
		out.println("background:Black;");
		// out.println("border: 1px solid #33AFFF;");
		out.println("color: #1fb4f7;");
		out.println("font-family: Helvetica, Arial, sans-serif;");
		out.println("}");
		out.println("</style>");
		out.println("<style>");

		out.println("table {");
		out.println("border=\"1|0\";border-color:Black;");
		out.println("width: 100%;height:50px;overflow:scroll;");

		out.println("font-family: Helvetica, Arial, sans-serif;");
		out.println("}");
		out.println("");
		out.println("th, td {");
		out.println("text-align: left;");
		out.println("padding: 8px;font-size:9px;");
		out.println("}");
		out.println("");
		out.println("tr:nth-child(even){background-color: #f2f2f2}");
		out.println("");
		out.println("th {");
		out.println("text-align: left;");
		out.println("background-color: White;");
		out.println("color: #1fb4f7; font-weight: bold;");
		out.println("}");
		out.println("</style>");
		out.println("<style>");
		out.println(
				".button { display: inline-block; padding: 10px 20px;font-size: 11px; cursor: pointer; text-align: center;text-decoration: none; outline: none;color: #fff;background-color: #1fb4f7;;"
						+ " border: none; border-radius: 14px; box-shadow: 0 5px #999; float: right; font-family: Helvetica, Arial, sans-serif; font-weight: bold; }");

		out.println(".button:hover {background-color: #1fb4f7;}"
				+ ".button:active { background-color: #1fb4f7;; box-shadow: 0 3px #666; transform: translateY(4px);}");
		/*
		 * out.println(".shadow {" +
		 * "                    -moz-box-shadow: inset 0 0 5px #888;" +
		 * "-webkit-box-shadow: inset 0 0 5px#888;" +
		 * "box-shadow: inner 0 0 5px #888;" + "               }");
		 */
		out.println(
				".scrollit {" + "                overflow:scroll;" + "               height:700px;" + "            }");
		out.println("body" + "            {" + "                  font-family: arial, helvetica, freesans, sans-serif;"
				+ "                  font-size: 100%;" + "                    color: #333;font-weight:bold"
				+ "                  background-color: #ddd;" + "             }");

		out.println(".box" + "           {" + "                  position: relative;"
				+ "                  width: 1000px;                    padding: 25px;"
				+ "                  margin: 0 auto;" + "                  background-color: #fff;"
				+ "                  -webkit-box-shadow: 0 0 4px rgba(0, 0, 0, 0.2), inset 0 0 50px rgba(0, 0, 0, 0.1);"
				+ "                  -moz-box-shadow: 0 0 4px rgba(0, 0, 0, 0.2), inset 0 0 50px rgba(0, 0, 0, 0.1);"
				+ "                  box-shadow: 0 0 5px rgba(0, 0, 0, 0.2), inset 0 0 50px rgba(0, 0, 0, 0.1);"
				+ "           }");

		out.println(".box:before, .box:after"
				+ "           {                    position: absolute;               width: 40%;                height: 10px;              content: ' ';              left: 12px;              bottom: 12px;"
				+ "                  background: transparent;                 -webkit-transform: skew(-5deg) rotate(-5deg);                  -moz-transform: skew(-5deg) rotate(-5deg);"
				+ "                  -ms-transform: skew(-5deg) rotate(-5deg);              -o-transform: skew(-5deg) rotate(-5deg);        transform: skew(-5deg) rotate(-5deg);                  -webkit-box-shadow: 0 6px 12px rgba(0, 0, 0, 0.3);"
				+ "                  -moz-box-shadow: 0 6px 12px rgba(0, 0, 0, 0.3);               box-shadow: 0 6px 12px rgba(0, 0, 0, 0.3);                    z-index: -1;"
				+ "           } ");

		out.println(".box:after"
				+ "           {                    left: auto;                right: 12px;                     -webkit-transform: skew(5deg) rotate(5deg);                   -moz-transform: skew(5deg) rotate(5deg);"
				+ "                  -ms-transform: skew(5deg) rotate(5deg);                -o-transform: skew(5deg) rotate(5deg);"
				+ "                  transform: skew(5deg) rotate(5deg);" + "        } ");
		out.println("</style>");

		out.println("</head>");
		out.println("");

		String pattern_severity = "High|Medium|Low|Negligible";
		Pattern r = Pattern.compile(pattern_severity);

		String pattern_id = "(^[CVE-]+[0-9]{4}-[0-9]{4})";
		Pattern r1 = Pattern.compile(pattern_id);

		String desc = "(^[CVE]+[-].*)";
		Pattern strDesc = Pattern.compile(desc);

		String pattern_pack = "(^Package:.*)";
		Pattern r2 = Pattern.compile(pattern_pack);

		String pattern_link = "(^Link:.*)";
		Pattern r3 = Pattern.compile(pattern_link);

		String pattern_layer = "(^Layer:.*)";
		Pattern r4 = Pattern.compile(pattern_layer);

		Matcher m = null;
		Matcher m1 = null;
		Matcher m2 = null;
		Matcher m3 = null;
		Matcher m4 = null;
		Matcher d = null;

		int i = 1;

		String str_ID[] = new String[stringArr.length];
		String str_Pack[] = new String[stringArr.length];
		String str_Link[] = new String[stringArr.length];
		String str_Layer[] = new String[stringArr.length];
		String str_Severe[] = new String[stringArr.length];
		String str_size[] = new String[stringArr.length];
		String str = null;
		// System.out.println("string is length is " + str_ID.length);
		while (i <= stringArr.length - 1) {
			m = r.matcher(stringArr[i]);
			m1 = r1.matcher(stringArr[i]);
			m2 = r2.matcher(stringArr[i]);
			m3 = r3.matcher(stringArr[i]);
			m4 = r4.matcher(stringArr[i]);
			d = strDesc.matcher(stringArr[i]);

			if (d.find()) {
				String strNew = d.group();
				content = content.replace(strNew, "<td>");
			}

			if (m1.find()) {
				str_ID[i] = m1.group();
			}

			if (m.find()) {
				str_Severe[i] = m.group();
			}

			if (m2.find()) {
				str = m2.group();
				content = content.replace(str, "</td>");
				str = str.replaceAll("Package: ", "");
				str_Pack[i] = str;
			}

			if (m3.find()) {
				str = m3.group();
				content = content.replace(stringArr[i], "");
				str = str.replaceAll("Link: ", "");
				str_Link[i] = str;
			}

			if (m4.find()) {
				str = m4.group();
				content = content.replace(str, "");
				str = str.replaceAll("Layer: ", "");
				str_Layer[i] = str;
			}
			i++;
		}

		String[] arr1 = content.split("<td>");

		String strNew = "";
		for (int j = 1; j < arr1.length; j++) {
			strNew = strNew.concat(arr1[j]);
		}

		String[] arr2 = strNew.split("</td>");

		String str_Desc[] = new String[str_ID.length];
		str_Desc = arr2;

		ArrayList<String> listID = new ArrayList<String>();
		for (String s : str_ID)
			if (s != null)
				listID.add(s);

		ArrayList<String> listSevere = new ArrayList<String>();
		for (String s : str_Severe)
			if (s != null)
				listSevere.add(s);

		ArrayList<String> listPack = new ArrayList<String>();
		for (String s : str_Pack)
			if (s != null)
				listPack.add(s);

		ArrayList<String> listLink = new ArrayList<String>();
		for (String s : str_Link)
			if (s != null)
				listLink.add(s);

		ArrayList<String> listLayer = new ArrayList<String>();
		for (String s : str_Layer)
			if (s != null)
				listLayer.add(s);

		int counter = 0;

		for (int j = 0; j < listSevere.size(); j++) {

			if (listSevere.get(j).contains("Medium")) {
				counter++;

			}
		}
		// System.out.println("medium count is" + counter);
		str_ID = listID.toArray(new String[listID.size()]);
		str_Severe = listSevere.toArray(new String[listSevere.size()]);
		str_Pack = listPack.toArray(new String[listPack.size()]);
		str_Link = listLink.toArray(new String[listLink.size()]);

		List<String> finallayerlist = new ArrayList<String>();
		List<String> finalsizeoflayer = new ArrayList<String>();
		List<String> finallayerlist1 = new ArrayList<String>();
		List<String> finalsizeoflayer1 = new ArrayList<String>();

		System.out.println("list layer is" + listLayer);
		

			for (int j1 = 0; j1 < id.size(); j1++) {
				for (int j2 = 0; j2 < listLayer.size(); j2++) {
					if (listLayer.get(j2).contains(id.get(j1))) {
						// System.out.println("Listlayer -"+listLayer.get(j1)+
						// "::"+
						// "id list -"+id.get(j1)+":: Command is
						// -"+command.get(j1)
						// );

						finallayerlist.add(finalcommand.get(j1));
						finalsizeoflayer.add(sizeoflayer.get(j1));
						str_Layer = finallayerlist.toArray(new String[finallayerlist.size()]);
						str_size = finalsizeoflayer.toArray(new String[finalsizeoflayer.size()]);

					}

					else if (!listLayer.get(j2).contains(id.get(j1))) {
						finallayerlist1.add("Not Available");
						finalsizeoflayer1.add("null");
						str_Layer = finallayerlist1.toArray(new String[finallayerlist1.size()]);
						str_size = finalsizeoflayer1.toArray(new String[finalsizeoflayer1.size()]);
					}
				}
			}
		
		System.out.println("the final layer list is" + finallayerlist1);

		System.out.println("the final size of layer" + finalsizeoflayer1);
		/*
		 * str_Layer = finallayerlist.toArray(new
		 * String[finallayerlist.size()]); String str_size[] =
		 * finalsizeoflayer.toArray(new String[finalsizeoflayer.size()]);
		 */

		out.println("<body>");
		out.println("<div class=\"box\">");
		// out.println("<div id=\"test\" style=\"float:right;\">");
		out.println(
				"<button class=\"button\"; onclick=\"tablesToExcel(['tbl1','tbl2','tbl3','tbl4'], ['High','Medium','Low','Negligible'], 'Docker Security Report.xls', 'Excel')\">Export to Excel</button>");
		out.println(
				"<h4 style=\"color:#1fb4f7;font-family: Helvetica, Arial, sans-serif;font-size: 11px;font-weight: bold;\"><u><b>Severity Summary</b></u></h4>");
		out.println("<table style=\"width:30%; font-size: 11px;font-weight:bold\">");
		out.println("<tr style=\"font-weight:bold\">");
		out.println("<th>High</th>");
		out.println("<th>Medium</th>");
		out.println("<th>Low</th>");
		out.println("<th>Negligible</th>");
		out.println("</tr>");
		out.println("<tr>");
		out.println("<td><div  class=\"high-count\"></div></td>");
		out.println("<td><div  class=\"med-count\"></div></td>");
		out.println("<td><div  class=\"low-count\"></div></td>");
		out.println("<td><div  class=\"neg-count\"></div></td>");
		out.println("</tr>");
		out.println("");
		out.println("</table>");
		out.println("<br>");
		out.println("</table>");
		out.println("<br>");
		out.println(
				"<div id=\"curve_chart\" style=\"width: 430px; height: 200px; float:left;margin:20px;border: 2px solid black;box-sizing: border-box;\"/></div>");
		out.println(
				"<div  id=\"donutchart\" style=\"width: 425px;float:right; height: 200px;margin:20px;border: 2px solid black;box-sizing: border-box;\"></div>");
		// layers info
		out.println("<br>");

		out.println("<p></p>");
		out.println("<br><br><br><br>");
		out.println("<br><br><br><br>");

		out.println("<p></p>");
		out.println("<br><br><br><br>");
		out.println("<hr>");

		out.println(
				"<div  id=\"container\" style=\"width: 425px;float:left; height: 250px;margin:20px;border: 2px solid black;box-sizing: border-box;\"></div>");
		// dropdown on html page
		out.println(
				"<div id=\"dropdown\" style=\"width: 425px;float:right; height: 250px;margin:20px;border: 2px solid black;box-sizing: border-box;\">");

		out.println("<p><h5 style=\"text-align:right;\">Select the build number.");
		out.println("<select id=\"mySelect\" onchange=\"myFunction()\" style=\"text-align:right;\">");
		// handling cases

		if (buildNo == Integer.parseInt("0")) {
			if (layer_file_keyList.size() < 1) {
				listener.getLogger().println("No data available to draw the Graph");

			} else if (layer_file_keyList.size() > 0 && layer_file_keyList.size() < 5) {
				for (int j = 0; j < layer_file_keyList.size() - 1; j++) {
					out.println("<option value='" + layer_file_keyList.get(j) + "'>" + layer_file_keyList.get(j));
				}
				out.println(
						"<option selected=\"selected\" value='" + layer_file_keyList.get(layer_file_keyList.size() - 1)
								+ "'>" + layer_file_keyList.get(layer_file_keyList.size() - 1));

			} else {
				for (int j = layer_file_keyList.size() - 5; j < layer_file_keyList.size() - 1; j++) {
					out.println("<option value='" + layer_file_keyList.get(j) + "'>" + layer_file_keyList.get(j));
				}
				out.println(
						"<option selected=\"selected\" value='" + layer_file_keyList.get(layer_file_keyList.size() - 1)
								+ "'>" + layer_file_keyList.get(layer_file_keyList.size() - 1));

			}
		} else if (layer_file_keyList.size() >= buildNo) {
			for (int j = layer_file_keyList.size() - buildNo; j < layer_file_keyList.size() - 1; j++) {
				out.println("<option value='" + layer_file_keyList.get(j) + "'>" + layer_file_keyList.get(j));
			}
			out.println("<option selected=\"selected\" value='" + layer_file_keyList.get(layer_file_keyList.size() - 1)
					+ "'>" + layer_file_keyList.get(layer_file_keyList.size() - 1));

		} else {
			for (int j = 0; j < layer_file_keyList.size() - 1; j++) {
				out.println("<option value='" + layer_file_keyList.get(j) + "'>" + layer_file_keyList.get(j));
			}
			out.println("<option selected=\"selected\" value='" + layer_file_keyList.get(layer_file_keyList.size() - 1)
					+ "'>" + layer_file_keyList.get(layer_file_keyList.size() - 1));

		}

		/*
		 * for (int j = layer_file_keyList.size() - buildNo; j <
		 * layer_file_keyList.size() ; j++) { out.println("<option value='" +
		 * layer_file_keyList.get(j) + "'>" + layer_file_keyList.get(j)); }
		 */

		out.println("</select>");
		out.println("<div id=\"container1\" style=\"width: 400px;float:left; height:190px;margin:auto;\">");

		out.println("</div>");
		out.println("</div>");
		out.println("<br><br><br><br><br><br><br><br><br><br><br><br><br><br><br><br><br><br><br>");
		// drpdown end

		out.println(
				"<h2 style=\"color:#1fb4f7;font-family: Helvetica, Arial, sans-serif;font-size: 11px;font-weight: bold;\"><u><b>Severity Details</b></u></h2>");
		out.println(
				"<h4 style=\"font-size:11px;\">Click on each Severity level to know more about the Severity details specific to that level.</h4>");
		out.println("<div id=\"accordion\">");
		out.println("<h2 style=\"font-size:11px;\">High</h2>");
		out.println("<div id = \"tabs-2\" class=\"scrollit\">");
		out.println("<table id=\"tbl1\">");
		out.println("<tr>");
		out.println("<th align=\"center\">ID</th>");
		out.println("<th align=\"center\">Description</th>");
		out.println("<th align=\"center\">Package</th>");
		out.println("<th align=\"center\">Link</th>");
		out.println("<th></th>");
		out.println("<th align=\"center\">Layer</th>");
		out.println("<th align=\"center\">Size of Layer</th>");
		out.println("</tr>");

		int j = 0;
		while (j < str_Severe.length - 1) {
			// System.out.println("inside whilee, Hiiiiiiiii");
			// System.out.println("j value is "+j);
			if (str_Severe[j].equals("High")) {
				// System.out.println("inside iffffff");
				out.println("<tr>");
				out.println("<td width=\"10%\">" + "<font color=\"#ff6600  \">" + str_ID[j] + "</font>" + "</td>");
				out.println("<td width=\"45%\">" + str_Desc[j] + "</td>");
				out.println("<td width=\"20%\">" + str_Pack[j] + "</td>");
				out.println("<td><a href=\"" + str_Link[j] + "\">" + str_Link[j] + "</a></td>");
				out.println("<td></td>");
				out.println("<td width=\"20%\">" + str_Layer[j] + "</td>");
				out.println("<td width=\"30%\">" + str_size[j] + "</td>");
				out.println("</tr>");

				countHigh++;

			}
			j++;
		}
		System.out.println("High count is" + countHigh);
		out.println("</table>");
		out.println("");
		out.println("</div>");
		out.println("<h2 style=\"font-size:11px;\">Medium</h2>");
		out.println("<div id = \"tabs-3\" class=\"scrollit\">");
		out.println("<table id=\"tbl2\">");
		out.println("<tr>");
		out.println("<th align=\"center\">ID</th>");
		out.println("<th align=\"center\">Description</th>");
		out.println("<th align=\"center\">Package</th>");
		out.println("<th align=\"center\">Link</th>");
		out.println("<th></th>");
		out.println("<th align=\"center\">Layer</th>");
		out.println("<th>Size of Layer</th>");
		out.println("</tr>");

		j = 0;
		while (j < str_Severe.length - 1) {
			if (str_Severe[j].equals("Medium")) {

				out.println("<tr>");
				out.println("<td width=\"10%\">" + "<font color=\"#ff6600 \">" + str_ID[j] + "</font>" + "</td>");
				out.println("<td width=\"45%\">" + str_Desc[j] + "</td>");
				out.println("<td width=\"20%\">" + str_Pack[j] + "</td>");
				out.println("<td><a href=\"" + str_Link[j] + "\">" + str_Link[j] + "</a></td>");
				out.println("<td></td>");
				out.println("<td width=\"20%\">" + str_Layer[j] + "</td>");
				out.println("<td width=\"30%\">" + str_size[j] + "</td>");

				out.println("</tr>");

				countMed++;
			}
			j++;
		}
		// System.out.println("Low count is " + countMed);
		out.println("</table>");
		out.println("</div>");
		out.println("<h2 style=\"font-size:11px;\">Low</h2>");
		out.println("<div id = \"tabs-4\" class=\"scrollit\">");

		out.println("<table id=\"tbl3\">");
		out.println("<tr>");
		out.println("<th>ID</th>");
		out.println("<th>Description</th>");
		out.println("<th>Package</th>");
		out.println("<th>Link</th>");
		out.println("<th></th>");
		out.println("<th>Layer</th>");
		out.println("<th>Size of Layer</th>");
		out.println("</tr>");

		j = 0;
		while (j < str_Severe.length - 1) {
			if (str_Severe[j].equals("Low")) {

				out.println("<tr>");
				out.println("<td width=\"10%\">" + "<font color=\"#ff6600\">" + str_ID[j] + "</font>" + "</td>");
				out.println("<td width=\"45%\">" + str_Desc[j] + "</td>");
				out.println("<td width=\"20%\">" + str_Pack[j] + "</td>");
				out.println("<td><a href=\"" + str_Link[j] + "\">" + str_Link[j] + "</a></td>");
				out.println("<td></td>");
				out.println("<td width=\"20%\">" + str_Layer[j] + "</td>");
				out.println("<td width=\"30%\">" + str_size[j] + "</td>");
				out.println("</tr>");

				countLow++;
			}
			j++;
		}
		// System.out.println("low count is " + countLow);
		out.println("</table>");
		out.println("</div>");
		out.println("<h2 style=\"font-size:11px;\">Negligible</h2>");
		out.println("<div id = \"tabs-5\" class=\"scrollit\">");

		out.println("<table id=\"tbl4\">");
		out.println("<tr>");
		out.println("<th>ID</th>");
		out.println("<th>Description</th>");
		out.println("<th>Package</th>");
		out.println("<th>Link</th>");
		out.println("<th></th>");
		out.println("<th>Layer</th>");
		out.println("<th>Size of Layer</th>");
		out.println("</tr>");

		j = 0;
		while (j < str_Severe.length - 1) {
			if (str_Severe[j].equals("Negligible")) {

				out.println("<tr>");
				out.println("<td width=\"10%\">" + "<font color=\"#ff6600\">" + str_ID[j] + "</font>" + "</td>");
				out.println("<td width=\"45%\">" + str_Desc[j] + "</td>");
				out.println("<td width=\"20%\">" + str_Pack[j] + "</td>");
				out.println("<td><a href=\"" + str_Link[j] + "\">" + str_Link[j] + "</a></td>");
				out.println("<td></td>");
				out.println("<td width=\"20%\">" + str_Layer[j] + "</td>");
				out.println("<td width=\"30%\">" + str_size[j] + "</td>");
				out.println("</tr>");

				countNeg++;
			}
			j++;
		}
		// System.out.println("neg count is " + countNeg);
		out.println("</table>");
		out.println("</div>");
		out.println("</div>");
		out.println("</div>");

		out.println("</body>");

		out.println("<script type=\"text/javascript\">");
		out.println("$('div.high-count').text('" + countHigh + "');");
		out.println("$('div.med-count').text('" + countMed + "');");
		out.println("$('div.low-count').text('" + countLow + "');");
		out.println("$('div.neg-count').text('" + countNeg + "');");
		out.println("</script>");

		// draw pie chart
		out.println("<script type=\"text/javascript\" src=\"https://www.gstatic.com/charts/loader.js\"></script>");
		out.println("<script type=\"text/javascript\">");
		out.println("google.charts.load(\"current\", {packages:[\"corechart\"]});"
				+ "google.charts.setOnLoadCallback(drawChart);");
		out.println("function drawChart() {var data = google.visualization.arrayToDataTable(["
				+ "              ['Type', 'Vulnarability'], ['High', " + countHigh + "], ['Medium', " + countMed + "],"
				+ "     ['Low'," + countLow + "],     ['Negligible'," + countNeg + "]    ]);"
				+ "   var options = {  pieHole: 0.3 };");

		out.println("  var chart = new google.visualization.PieChart(document.getElementById('donutchart'));");
		out.println("chart.draw(data, options); }");
		out.println("  </script>");

		out.println("<script type=\"text/javascript\" src=\"https://www.google.com/jsapi\"></script>");
		out.println("<script type=\"text/javascript\">");
		out.println("google.load(\"visualization\", \"1\", {packages:[\"corechart\"]});"

				+ "google.charts.setOnLoadCallback(drawChart);");
		// draw line chart
		Properties props = new Properties();
		File file1 = Values_File.valuesFile(countHigh, countMed, countLow, countNeg, build_no, build, jenkins_home,
				listener);
		FileInputStream fis = new FileInputStream(file1);
		props.load(fis);
		Set<Object> keys = props.keySet();
		List<Object> listKeys1 = new ArrayList<Object>();
		int high = 0;
		int med = 0;
		int low = 0;
		int neg = 0;

		listKeys1.addAll(keys);
		List<Integer> listKeys = new ArrayList<Integer>();
		for (Object object : listKeys1) {
			String obj = object.toString();
			Integer intobj = Integer.parseInt(obj);
			listKeys.add(intobj);
		}
		Collections.sort(listKeys);
		System.out.println("sorted listkeys " + listKeys);
		int sizeOfLoop = keys.size();
		out.println(
				"function drawChart() {var data = new google.visualization.DataTable();data.addColumn('string', 'Severity');data.addColumn('number', 'High'); data.addColumn('number', 'Medium');data.addColumn('number', 'Low'); data.addColumn('number', 'Negligible');");

		if (buildNo == Integer.parseInt("0")) {
			if (listKeys.size() < 1) {
				listener.getLogger().println("No data available to draw the Graph");
			}

			else if (listKeys.size() > 0 && listKeys.size() < 5) {
				if (sizeOfLoop == 1) {
					out.println("data.addRows(" + 1 + ");");
					int index1 = 0;

					for (int z = 0; z < sizeOfLoop; z++) {
						String datavalue = props.getProperty(listKeys.get(z).toString()).trim();
						int key = Integer.parseInt(listKeys.get(z).toString());
						String[] array = datavalue.split("\\,");
						for (int n = 0; n < 4; n++) {
							high = Integer.parseInt(array[0].trim());
							med = Integer.parseInt(array[1].trim());
							low = Integer.parseInt(array[2].trim());
							neg = Integer.parseInt(array[3].trim());

						}
						out.println("data.setValue(" + index1 + "," + 0 + ", '" + listKeys.get(z) + "');");
						out.println("data.setValue(" + index1 + "," + 1 + "," + high + ");");
						out.println("data.setValue(" + index1 + "," + 2 + "," + med + ");");
						out.println("data.setValue(" + index1 + "," + 3 + "," + low + ");");
						out.println("data.setValue(" + index1 + "," + 4 + "," + neg + ");");

					}
				} else {
					out.println("data.addRows(" + sizeOfLoop + ");");
					int index1 = 0;
					while (index1 < sizeOfLoop) {
						for (int k = 0; k < sizeOfLoop; k++) {
							String datavalue = props.getProperty(((listKeys.get(k)).toString()).trim());
							int key = Integer.parseInt(((listKeys.get(k)).toString()));
							String[] array = datavalue.split("\\,");
							for (int n = 0; n < 4; n++) {
								high = Integer.parseInt(array[0].trim());
								med = Integer.parseInt(array[1].trim());
								low = Integer.parseInt(array[2].trim());
								neg = Integer.parseInt(array[3].trim());
							}
							out.println("data.setValue(" + index1 + "," + 0 + ", '" + listKeys.get(k) + "');");
							out.println("data.setValue(" + index1 + "," + 1 + "," + high + ");");
							out.println("data.setValue(" + index1 + "," + 2 + "," + med + ");");
							out.println("data.setValue(" + index1 + "," + 3 + "," + low + ");");
							out.println("data.setValue(" + index1 + "," + 4 + "," + neg + ");");
							index1++;
						}
					}

				}
				out.println("var options = {title: 'Docker Security Severity Trend',legend: { position: 'bottom' }};");
				out.println("var chart = new google.visualization.LineChart(document.getElementById('curve_chart'))");
				out.println("chart.draw(data, options); }");
				fis.close();
				out.println("</script>");
			}

			else {
				out.println("data.addRows(" + 5 + ");");
				int index1 = 0;
				while (index1 < 5) {
					for (int k = (listKeys.size() - 5); k < (listKeys.size()); k++) {
						String datavalue = props.getProperty(((listKeys.get(k)).toString()).trim());
						int key = Integer.parseInt(((listKeys.get(k)).toString()));
						String[] array = datavalue.split("\\,");
						for (int n = 0; n < 4; n++) {
							high = Integer.parseInt(array[0].trim());
							med = Integer.parseInt(array[1].trim());
							low = Integer.parseInt(array[2].trim());
							neg = Integer.parseInt(array[3].trim());
						}
						out.println("data.setValue(" + index1 + "," + 0 + ", '" + listKeys.get(k) + "');");
						out.println("data.setValue(" + index1 + "," + 1 + "," + high + ");");
						out.println("data.setValue(" + index1 + "," + 2 + "," + med + ");");
						out.println("data.setValue(" + index1 + "," + 3 + "," + low + ");");
						out.println("data.setValue(" + index1 + "," + 4 + "," + neg + ");");
						index1++;
					}
				}
				out.println("var options = {title: 'Docker Security Severity Trend',legend: { position: 'bottom' }};");
				out.println("var chart = new google.visualization.LineChart(document.getElementById('curve_chart'))");
				out.println("chart.draw(data, options); }");
				fis.close();
				out.println("</script>");

			}
		}

		else if (listKeys.size() >= buildNo) {

			out.println("data.addRows(" + buildNo + ");");
			int index1 = 0;
			while (index1 < buildNo) {
				for (int k = (listKeys.size() - buildNo); k < (listKeys.size()); k++) {
					String datavalue = props.getProperty(((listKeys.get(k)).toString()).trim());
					int key = Integer.parseInt(((listKeys.get(k)).toString()));
					String[] array = datavalue.split("\\,");
					for (int n = 0; n < 4; n++) {
						high = Integer.parseInt(array[0].trim());
						med = Integer.parseInt(array[1].trim());
						low = Integer.parseInt(array[2].trim());
						neg = Integer.parseInt(array[3].trim());
					}
					out.println("data.setValue(" + index1 + "," + 0 + ", '" + listKeys.get(k) + "');");
					out.println("data.setValue(" + index1 + "," + 1 + "," + high + ");");
					out.println("data.setValue(" + index1 + "," + 2 + "," + med + ");");
					out.println("data.setValue(" + index1 + "," + 3 + "," + low + ");");
					out.println("data.setValue(" + index1 + "," + 4 + "," + neg + ");");
					index1++;
				}

			}
			out.println("var options = {title: 'Docker Security Severity Trend',legend: { position: 'bottom' }};");
			out.println("var chart = new google.visualization.LineChart(document.getElementById('curve_chart'))");
			out.println("chart.draw(data, options); }");
			fis.close();
			out.println("</script>");

		} else {
			out.println("data.addRows(" + sizeOfLoop + ");");
			int index1 = 0;
			while (index1 < sizeOfLoop) {
				for (int z = 0; z < sizeOfLoop; z++) {
					String datavalue = props.getProperty(listKeys.get(z).toString()).trim();
					int key = Integer.parseInt(listKeys.get(z).toString());
					String[] array = datavalue.split("\\,");
					for (int n = 0; n < 4; n++) {
						high = Integer.parseInt(array[0].trim());
						med = Integer.parseInt(array[1].trim());
						low = Integer.parseInt(array[2].trim());
						neg = Integer.parseInt(array[3].trim());

					}
					out.println("data.setValue(" + index1 + "," + 0 + ", '" + listKeys.get(z) + "');");
					out.println("data.setValue(" + index1 + "," + 1 + "," + high + ");");
					out.println("data.setValue(" + index1 + "," + 2 + "," + med + ");");
					out.println("data.setValue(" + index1 + "," + 3 + "," + low + ");");
					out.println("data.setValue(" + index1 + "," + 4 + "," + neg + ");");
					index1++;
				}
			}

			out.println("var options = {title: 'Docker Security Severity Trend',legend: { position: 'bottom' }};");
			out.println("var chart = new google.visualization.LineChart(document.getElementById('curve_chart'))");
			out.println("chart.draw(data, options); }");
			fis.close();
			out.println("</script>");
		}
		// ends line chart

		// copying layer data to a prop file for image size graph
		/*
		 * String Filename = JobName + "_ImageSize_data.properties"; File writer
		 * = new File(build.getRootDir(), "print_stream"); FilePath workspace =
		 * build.getWorkspace(); FilePath his_target = new FilePath(workspace,
		 * Filename); FilePath outFile1 = new FilePath(writer);
		 * 
		 * String FILENAME = jenkins_home + "/" + Filename;
		 * 
		 * File file_his = new File(FILENAME); //
		 * System.out.println("Filename of properties file" + FILENAME);
		 * BufferedWriter bw = null; FileWriter fw = null; String data = null;
		 * 
		 * try {
		 * 
		 * data = (build_no + " = " + build_no + ", " + total_image_size +
		 * "\n");
		 * 
		 * // if file doesnt exists, then create it if (!file_his.exists()) {
		 * file_his.createNewFile(); }
		 * 
		 * // true = append file fw = new FileWriter(file_his.getAbsoluteFile(),
		 * true); bw = new BufferedWriter(fw);
		 * 
		 * bw.write(data);
		 * 
		 * System.out.println("Done");
		 * 
		 * } catch (IOException e) {
		 * 
		 * e.printStackTrace();
		 * 
		 * } finally {
		 * 
		 * try {
		 * 
		 * if (bw != null) bw.close();
		 * 
		 * if (fw != null) fw.close();
		 * 
		 * } catch (IOException ex) {
		 * 
		 * ex.printStackTrace();
		 * 
		 * } }
		 * 
		 * FilePath Filepath1 = new FilePath(file_his);
		 * Filepath1.copyTo(his_target);
		 */

		// fetching data from history_layer file
		Properties props1 = new Properties();
		FileInputStream fis_his = new FileInputStream(file_his);

		props1.load(fis_his);
		Set<Object> lay_keys = props1.keySet();

		List<Object> list_layer_Keys = new ArrayList<Object>();
		list_layer_Keys.addAll(lay_keys);
		List<Integer> listKeys2 = new ArrayList<Integer>();

		for (Object object1 : list_layer_Keys) {
			String obj1 = object1.toString();
			Integer intobj1 = Integer.parseInt(obj1);
			listKeys2.add(intobj1);
		}
		String[] command_value = null;

		Collections.sort(listKeys2);

		List<String> list_commands = new ArrayList<String>();
		List<String> image_name = new ArrayList<String>();
		List<String> image_size = new ArrayList<String>();
		int no_of_build = lay_keys.size();
		for (int k = 0; k < no_of_build; k++) {
			command_value = props1.getProperty(listKeys2.get(k).toString()).split(",");

		}

		// image size graph(begin)

		out.println("<script type=\"text/javascript\" src=\"https://www.gstatic.com/charts/loader.js\"></script>");
		out.println("<script type=\"text/javascript\">");
		out.println("google.charts.load(\'current\', {packages:[\'corechart\']})");
		out.println("</script>");
		out.println("</head>");
		out.println("<body>");
		out.println("<script language=\"JavaScript\">");
		out.println(
				"function drawChart() { var data = google.visualization.arrayToDataTable([['Build', 'Size', { role: 'style' } ],");
		String name_of_image = null;
		String Size_of_image = null;
		if (buildNo == Integer.parseInt("0")) {
			if (listKeys2.size() < 1) {
				listener.getLogger().println("No data available to draw the Graph");
			} else if (listKeys2.size() > 0 && listKeys2.size() < 5) {
				for (int k = 0; k < listKeys2.size(); k++) {
					command_value = props1.getProperty(listKeys2.get(k).toString()).split(",");
					out.println("['" + command_value[0] + "'," + Double.parseDouble(command_value[1])
							+ ",'color: #ff9900'],");
				}

			}

			else {
				for (int k = (listKeys2.size() - 5); k < (listKeys2.size()); k++) {
					command_value = props1.getProperty(listKeys2.get(k).toString()).split(",");
					out.println("['" + command_value[0] + "'," + Double.parseDouble(command_value[1])
							+ ",'color: #ff9900'],");
				}
			}
		} else if (listKeys2.size() >= buildNo) {

			for (int k = listKeys2.size() - buildNo; k < listKeys2.size(); k++) {
				command_value = props1.getProperty(listKeys2.get(k).toString()).split(",");
				out.println(
						"['" + command_value[0] + "'," + Double.parseDouble(command_value[1]) + ",'color: #ff9900'],");
			}
		} else {
			for (int k = 0; k < no_of_build; k++) {
				command_value = props1.getProperty(listKeys2.get(k).toString()).split(",");
				out.println(
						"['" + command_value[0] + "'," + Double.parseDouble(command_value[1]) + ",'color: #ff9900'],");

			}
		}

		out.println("]);");

		out.println(" var view = new google.visualization.DataView(data);");
		out.println(
				"view.setColumns([0, 1,{ calc: \"stringify\",sourceColumn: 1, type: \"string\",role: \"annotation\" }, 2]);");
		out.println(
				"var options = {title: \"Size of Images\", width: 410,height: 240,hAxis: { title: \'Build Number\'}, vAxis: {title: \'Size Of Image(Mb)\' },bar: {groupWidth: \"85%\"},legend: { position:\"none\" },tooltip: { isHtml: true },};");

		out.println("var chart = new google.visualization.ColumnChart(document.getElementById('container'));");
		out.println("chart.draw(view, options);");
		out.println("}");

		out.println("google.charts.setOnLoadCallback(drawChart);");
		out.println("</script>");
		// image size graph ends

		// dropdown graph (begin)
		out.println("<script type=\"text/javascript\" src=\"https://www.gstatic.com/charts/loader.js\"></script>");
		out.println("<script type=\"text/javascript\">");
		out.println("google.charts.load('current', {packages:['corechart']})");
		out.println("</script>");
		out.println("<script language=\"JavaScript\">");
		out.println("function myFunction() {");
		out.println(
				"var options = { title: 'Image Layers',isStacked:true,hAxis: { title: 'Build Number'}, vAxis: {title: 'Size Of Image(Mb)'}};");
		out.println("var chart = new google.visualization.ColumnChart(document.getElementById('container1'));");

		// data for dropdown graph
		if (buildNo == Integer.parseInt("0")) {
			if (layer_file_keyList.size() < 1) {
				listener.getLogger().println("No data available to draw the Graph");
			} else if (layer_file_keyList.size() > 0 && layer_file_keyList.size() < 5) {

				for (int k = 0; k < layer_file_keyList.size(); k++) {
					out.println("var data" + layer_file_keyList.get(k) + "= google.visualization.arrayToDataTable([");
					out.println("['Build',");
					layer_file_commands = props2.getProperty(layer_file_keyList.get(k).toString());
					System.out.println("layer file comamnd is " + layer_file_commands);
					String layfile_data[] = layer_file_commands.split("},");

					String layfile_data1[] = layfile_data[0].split(",");

					for (int k1 = 0; k1 < layfile_data1.length; k1++) {
						layer_file_command.add((layfile_data1[k1].replace("[ ", "")).replace(" ]", ""));
					}

					String layfile_data2[] = layfile_data[1].split(", ");

					for (int i1 = 0; i1 < layfile_data2.length; i1++) {

						layer_file_size.add((layfile_data2[i1].replace("[", "")).replace("]", ""));
					}
					// }
					System.out.println(layer_file_command);
					System.out.println(layer_file_size);
					for (int k2 = 0; k2 < layer_file_command.size(); k2++) {
						out.println("'" + layer_file_command.get(k2) + "',");
					}
					out.println("],");
					out.println("['" + layer_file_keyList.get(k) + "',");
					for (int k3 = 0; k3 < layer_file_size.size(); k3++) {
						out.println(layer_file_size.get(k3) + ",");
					}
					out.println("],");
					out.println("]);");
					layer_file_commands = null;
					layfile_data = null;
					layfile_data1 = null;
					layfile_data2 = null;
					layer_file_command.clear();
					layer_file_size.clear();
				}
			} else {
				for (int k = layer_file_keyList.size() - 5; k < layer_file_keyList.size(); k++) {
					out.println("var data" + layer_file_keyList.get(k) + "= google.visualization.arrayToDataTable([");
					out.println("['Build',");
					layer_file_commands = props2.getProperty(layer_file_keyList.get(k).toString());
					System.out.println("layer file comamnd is " + layer_file_commands);
					String layfile_data[] = layer_file_commands.split("},");

					String layfile_data1[] = layfile_data[0].split(",");

					for (int k1 = 0; k1 < layfile_data1.length; k1++) {
						layer_file_command.add((layfile_data1[k1].replace("[ ", "")).replace(" ]", ""));
					}

					String layfile_data2[] = layfile_data[1].split(", ");

					for (int i1 = 0; i1 < layfile_data2.length; i1++) {

						layer_file_size.add((layfile_data2[i1].replace("[", "")).replace("]", ""));
					}
					// }
					System.out.println(layer_file_command);
					System.out.println(layer_file_size);
					for (int k2 = 0; k2 < layer_file_command.size(); k2++) {
						out.println("'" + layer_file_command.get(k2) + "',");
					}
					out.println("],");
					out.println("['" + layer_file_keyList.get(k) + "',");
					for (int k3 = 0; k3 < layer_file_size.size(); k3++) {
						out.println(layer_file_size.get(k3) + ",");
					}
					out.println("],");
					out.println("]);");
					layer_file_commands = null;
					layfile_data = null;
					layfile_data1 = null;
					layfile_data2 = null;
					layer_file_command.clear();
					layer_file_size.clear();
				}

			}
		}

		else if (layer_file_keyList.size() >= buildNo) {
			for (int k = layer_file_keyList.size() - buildNo; k < layer_file_keyList.size(); k++) {
				out.println("var data" + layer_file_keyList.get(k) + "= google.visualization.arrayToDataTable([");
				out.println("['Build',");
				layer_file_commands = props2.getProperty(layer_file_keyList.get(k).toString());
				System.out.println("layer file comamnd is " + layer_file_commands);
				String layfile_data[] = layer_file_commands.split("},");

				String layfile_data1[] = layfile_data[0].split(",");

				for (int k1 = 0; k1 < layfile_data1.length; k1++) {
					layer_file_command.add((layfile_data1[k1].replace("[ ", "")).replace(" ]", ""));
				}

				String layfile_data2[] = layfile_data[1].split(", ");

				for (int i1 = 0; i1 < layfile_data2.length; i1++) {

					layer_file_size.add((layfile_data2[i1].replace("[", "")).replace("]", ""));
				}
				// }
				System.out.println(layer_file_command);
				System.out.println(layer_file_size);
				for (int k2 = 0; k2 < layer_file_command.size(); k2++) {
					out.println("'" + layer_file_command.get(k2) + "',");
				}
				out.println("],");
				out.println("['" + layer_file_keyList.get(k) + "',");
				for (int k3 = 0; k3 < layer_file_size.size(); k3++) {
					out.println(layer_file_size.get(k3) + ",");
				}
				out.println("],");
				out.println("]);");
				layer_file_commands = null;
				layfile_data = null;
				layfile_data1 = null;
				layfile_data2 = null;
				layer_file_command.clear();
				layer_file_size.clear();
			}

		} else {

			for (int k = 0; k < layer_file_keyList.size(); k++) {
				out.println("var data" + layer_file_keyList.get(k) + "= google.visualization.arrayToDataTable([");
				out.println("['Build',");
				layer_file_commands = props2.getProperty(layer_file_keyList.get(k).toString());
				System.out.println("layer file comamnd is " + layer_file_commands);
				String layfile_data[] = layer_file_commands.split("},");

				String layfile_data1[] = layfile_data[0].split(",");

				for (int k1 = 0; k1 < layfile_data1.length; k1++) {
					layer_file_command.add((layfile_data1[k1].replace("[ ", "")).replace(" ]", ""));
				}

				String layfile_data2[] = layfile_data[1].split(", ");

				for (int i1 = 0; i1 < layfile_data2.length; i1++) {

					layer_file_size.add((layfile_data2[i1].replace("[", "")).replace("]", ""));
				}
				// }
				System.out.println(layer_file_command);
				System.out.println(layer_file_size);
				for (int k2 = 0; k2 < layer_file_command.size(); k2++) {
					out.println("'" + layer_file_command.get(k2) + "',");
				}
				out.println("],");
				out.println("['" + layer_file_keyList.get(k) + "',");
				for (int k3 = 0; k3 < layer_file_size.size(); k3++) {
					out.println(layer_file_size.get(k3) + ",");
				}
				out.println("],");
				out.println("]);");
				layer_file_commands = null;
				layfile_data = null;
				layfile_data1 = null;
				layfile_data2 = null;
				layer_file_command.clear();
				layer_file_size.clear();
			}

		}

		if (buildNo == Integer.parseInt("0")) {
			if (layer_file_keyList.size() < 1) {
				listener.getLogger().println("No data available to draw the Graph");
			} else if (layer_file_keyList.size() > 0 && layer_file_keyList.size() < 5) {

				for (int k = 0; k < layer_file_keyList.size(); k++) {
					out.println("if(document.getElementById(\"mySelect\").value=='" + layer_file_keyList.get(k) + "')");
					out.println("{" + " chart.draw(data" + layer_file_keyList.get(k) + ", options);" + "} ");

				}
			} else {
				for (int k = layer_file_keyList.size() - 5; k < layer_file_keyList.size(); k++) {
					out.println("if(document.getElementById(\"mySelect\").value=='" + layer_file_keyList.get(k) + "')");
					out.println("{" + " chart.draw(data" + layer_file_keyList.get(k) + ", options);" + "} "); // calls
																												// the
				} // data
					// function
					// above
			}
		} else if (layer_file_keyList.size() >= buildNo) {
			for (int k = layer_file_keyList.size() - buildNo; k < layer_file_keyList.size(); k++) {
				out.println("if(document.getElementById(\"mySelect\").value=='" + layer_file_keyList.get(k) + "')");
				out.println("{" + " chart.draw(data" + layer_file_keyList.get(k) + ", options);" + "} "); // calls
																											// the
																											// data
																											// function
																											// above

			}
		} else {
			for (int k = 0; k < layer_file_keyList.size(); k++) {
				out.println("if(document.getElementById(\"mySelect\").value=='" + layer_file_keyList.get(k) + "')");
				out.println("{" + " chart.draw(data" + layer_file_keyList.get(k) + ", options);" + "} "); // calls
																											// the
																											// data
																											// function
																											// above

			}
		}
		/*
		 * for (int k = layer_file_keyList.size() - buildNo; k <
		 * layer_file_keyList.size(); k++) {
		 * out.println("if(document.getElementById(\"mySelect\").value=='" +
		 * layer_file_keyList.get(k) + "')"); out.println("{" +
		 * " chart.draw(data" + layer_file_keyList.get(k) + ", options);" +
		 * "} "); // calls // the // data // function // above
		 * 
		 * }
		 */
		out.println("}");

		out.println("google.charts.setOnLoadCallback(drawChart);");

		out.println("</script>");

		// dropdown graph ends
		// by default graph without dropdown selection

		out.println("<script type=\"text/javascript\" src=\"https://www.gstatic.com/charts/loader.js\"></script>");
		out.println("<script type=\"text/javascript\">");
		out.println("google.charts.load('current', {packages:['corechart']})");
		out.println("</script>");
		out.println("<script language=\"JavaScript\">");
		out.println("function drawChart() {");
		out.println("var data" + layer_file_keyList.get(layer_file_keyList.size() - 1)
				+ "= google.visualization.arrayToDataTable([");
		out.println("['Build',");

		layer_file_commands = props2.getProperty(layer_file_keyList.get(layer_file_keyList.size() - 1).toString());
		System.out.println("layer file comamnd is " + layer_file_commands);
		String layfile_data[] = layer_file_commands.split("},");

		String layfile_data1[] = layfile_data[0].split(",");

		for (int k1 = 0; k1 < layfile_data1.length; k1++) {
			layer_file_command.add((layfile_data1[k1].replace("[ ", "")).replace(" ]", ""));
		}

		String layfile_data2[] = layfile_data[1].split(", ");

		for (int i1 = 0; i1 < layfile_data2.length; i1++) {

			layer_file_size.add((layfile_data2[i1].replace("[", "")).replace("]", ""));
		}
		// }
		System.out.println(layer_file_command);
		System.out.println(layer_file_size);
		for (int k2 = 0; k2 < layer_file_command.size(); k2++) {
			out.println("'" + layer_file_command.get(k2) + "',");
		}
		out.println("],");
		out.println("['" + layer_file_keyList.get(layer_file_keyList.size() - 1) + "',");
		for (int k3 = 0; k3 < layer_file_size.size(); k3++) {
			out.println(layer_file_size.get(k3) + ",");
		}
		out.println("],");
		out.println("]);");
		layer_file_commands = null;
		layfile_data = null;
		layfile_data1 = null;
		layfile_data2 = null;
		layer_file_command.clear();
		layer_file_size.clear();

		out.println(
				"var options = { title: 'Image Layers',isStacked:true,hAxis: { title: 'Build Number'}, vAxis: {title: 'Size Of Image(Mb)'}};");
		out.println("var chart = new google.visualization.ColumnChart(document.getElementById('container1'));");
		out.println("chart.draw(data" + layer_file_keyList.get(layer_file_keyList.size() - 1) + ", options);");
		out.println("}");
		out.println("google.charts.setOnLoadCallback(drawChart);");

		out.println("</script>");

		// ends here
		out.println("</html>");
		out.println("");
		out.println("");
		out.close();
		return outfilFilePath1;

	}

	public static boolean checkQualityGate(int high, int low, int medium, Boolean Severity, AbstractBuild build) {
		// System.out.println(countHigh + "," + Text_HTMLConverter.countMed +
		// "," + Text_HTMLConverter.countLow);
		boolean result = false;
		int c = 0;
		if (Severity != false) {
			if (countHigh > high || countLow > low || countMed > medium) {
				result = true;
			}

		}
		countHigh = 0;
		countLow = 0;
		countMed = 0;
		countNeg = 0;
		return result;
	}
}
