import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;

import javax.microedition.lcdui.*;
import javax.microedition.midlet.MIDlet;
import javax.microedition.midlet.MIDletStateChangeException;


public final class PIMCMidlet extends MIDlet implements CommandListener {

	/* UI elements */
	private Display	display;
	private Command	cmdNext;	// go to the next screen
	private Command	cmdPrev;	// go to the previous screen
	private Command	cmdReset;	// go to the first screen
	private Command	cmdCompute;	// show compute total screen
	private Command cmdExit;	// exit command
	private Command cmdComments;	// more comments about the insurance
	private Command	cmdAbout;	// show about screen
	private List	lstChoices;	// list of choices
	private List	lstBuffer[];	// we take a double buffer approach to show lists!
	private Form	frmResult;	// displays the computation results
	private StringItem		itmTotal;	// display the total cost in the result form
	private StringItem[]	itmSummary;	// lines of summary report
	private StringItem		itmSummaryHeader; 	// summary table header
	private Alert	alertAbout;		// about screen
	private Alert	alertComments;	// comments alert
	
	/* data elements */
	private int curStep;	// current step in showing lists
	private int insurCount;	// number of steps
	private int levelCount;	// number of insurance levels available
	private int curBuffer; // current buffer. zero or one.
	private int choice[];	// selected choice in each step, starting from 0. -1 means no choice.
	private long[][] insurance;	// maximum level of insurance compensation
	private long[][] cost;		// monthly per capita insurance fee
	private String[][]	formattedInsur;	// insurance levels, formatted (thousand seperator)
	private String[]	titles;		// page titles
	private String[]	comments;	// insurance comments
	private int dataVersion;		// version of data with format: yymmddn
	
	public PIMCMidlet() {
		display = Display.getDisplay(this);
		try {
			readData();
			readTitles();
		} catch (IOException e) {
			Alert alertError = new Alert(e.getMessage());
			alertError.setType(AlertType.ERROR);
			alertError.setTimeout(Alert.FOREVER);
			display.setCurrent(new Alert(e.getMessage()));
			return;
		}
		initiUIElements();
		lstChoices = populateList(0, 0, levelCount, false, lstBuffer[0]);
		lstChoices.setSelectedIndex(0, true);
		display.setCurrent(lstChoices);
//		showSplash();
	}

	private void showSplash() {
		Alert splash = new Alert("");
		splash.setType(AlertType.INFO);
		splash.setString(StrTab.SPLASH_TEXT);		
		splash.setTimeout(Alert.FOREVER);
		display.setCurrent(splash, lstChoices);	
	}

	private void initiUIElements() {
		// init commands
		cmdNext		= new Command(StrTab.NEXT, Command.ITEM, 0);
		cmdCompute	= new Command(StrTab.COMPUTE, Command.ITEM, 0);
		cmdPrev		= new Command(StrTab.PREV, Command.BACK, 0);
		cmdExit		= new Command(StrTab.EXIT, Command.EXIT, 1);
		cmdReset	= new Command(StrTab.RESET, Command.SCREEN, 1);
		cmdComments = new Command(StrTab.COMMENTS, Command.SCREEN, 2);
		cmdAbout	= new Command(StrTab.ABOUT, Command.SCREEN, 3);
		
		// init lists
		lstBuffer = new List[2]; // double buffer
		lstBuffer[0] = new List("List 1", List.IMPLICIT);
		lstBuffer[1] = new List("List 2", List.IMPLICIT);
		
		// add common commands to lists
		for (int i = 0; i < 2; i++) {
			lstBuffer[i].addCommand(cmdAbout);
			lstBuffer[i].addCommand(cmdExit);
			lstBuffer[i].addCommand(cmdComments);
			lstBuffer[i].setCommandListener(this);
		}
		
		// init result form
		frmResult = new Form(StrTab.RESULT_FORM_TITLE);
		frmResult.addCommand(cmdAbout);
		frmResult.addCommand(cmdExit);
		frmResult.addCommand(cmdReset);
		frmResult.addCommand(cmdPrev);
		
		// this item shows the computed total cost
		itmTotal = new StringItem(StrTab.TOTAL_ITEM_LABEL, "");
		itmTotal.setLayout(Item.LAYOUT_2 | Item.LAYOUT_CENTER | Item.LAYOUT_NEWLINE_AFTER);
		itmTotal.setPreferredSize(frmResult.getWidth(), -1);
		frmResult.append(itmTotal);
		frmResult.setCommandListener(this);
		
		// summary header
		frmResult.append(new Spacer(frmResult.getWidth(), itmTotal.getMinimumHeight() / 2));
		itmSummaryHeader = new StringItem(null, StrTab.SUMMARY_HEADER);
		itmSummaryHeader.setLayout(Item.LAYOUT_2 | Item.LAYOUT_CENTER | Item.LAYOUT_NEWLINE_AFTER);
		itmSummaryHeader.setPreferredSize(frmResult.getWidth(), -1);
		frmResult.append(itmSummaryHeader);
		// summary report lines
		itmSummary = new StringItem[insurCount];
		for (int i = 0; i < insurCount; i++) {
			itmSummary[i] = new StringItem(null, "");
			itmSummary[i].setPreferredSize(frmResult.getWidth(), -1);
			itmSummary[i].setLayout(Item.LAYOUT_RIGHT | Item.LAYOUT_NEWLINE_AFTER
					| Item.LAYOUT_2);
			frmResult.append(itmSummary[i]);
		}
		
		// init about alert
		alertAbout = new Alert(StrTab.ABOUT);
		alertAbout.setType(AlertType.INFO);
		alertAbout.setTimeout(Alert.FOREVER);
		alertAbout.setString(StrTab.ABOUT_TEXT);
		
		// init comments alert
		alertComments = new Alert(StrTab.COMMENTS);
		alertComments.setType(AlertType.INFO);
		alertComments.setTimeout(Alert.FOREVER);
		
	}

	private void readData() throws IOException {
		InputStream is = this.getClass().getResourceAsStream("/Data.txt");		
		if (is == null)
			throw new IOException("Resource fil not found.");

		// for reading UTF strings
		InputStreamReader in = new InputStreamReader(is, "utf-8");
		nextStr(in); // read and discard the first line
		
		curStep		= 0;
		curBuffer	= 0;
		dataVersion = nextInt(in);
		insurCount	= nextInt(in);
		levelCount	= nextInt(in);
		choice 		= new int[insurCount];
		insurance	= new long[insurCount][levelCount];
		formattedInsur = new String[insurCount][levelCount];
		cost		= new long[insurCount][levelCount];
		
		for (int i = 0; i < levelCount; i++) {
			for (int j = 0; j < insurCount; j++) {
				insurance[j][i] = nextInt(in);
				formattedInsur[j][i] = Utils.formatNumber(insurance[j][i]);
				cost[j][i] = nextInt(in);
			}			
		}
		
		// init choices, first choice (0) for the first step, and no choice (-1) for others
		choice[0] = 0;
		for (int i = 1; i < insurCount; i++) {
			choice[i] = -1;
		}
	}
	
	private void readTitles() throws IOException {		
		InputStream is = this.getClass().getResourceAsStream("/Strings.txt");
		if (is == null)
			throw new IOException("Resource fil not found.");
		
		// for reading UTF strings
		InputStreamReader in = new InputStreamReader(is, "utf-8");		
		nextStr(in); // read and discard the first line
		
		int count = nextInt(in);
		titles = new String[count];
		comments = new String[count];
		
		for (int i = 0; i < count; i++) {
			titles[i] = nextStr(in);
			comments[i] = nextStr(in);
		}

	}
		
	private String nextStr(Reader in) throws IOException {
		
		int n = in.read();
		if (n == -1)
			return null;
		
		StringBuffer sb = new StringBuffer();
		while (n != -1) {
			char ch = (char)n;
			if (ch == '\t' || ch == '\n') break; // end of piece
			if (ch != '\r') // skip carriage return (\r)
				sb.append(ch);
			n = in.read();
		}
		return sb.toString();
	}
	
	private int nextInt(Reader in) throws IOException {
		String s = nextStr(in);
		if (s == null)
			return -1;
		else
			return Integer.parseInt(s);
	}
	
	private List populateList(int step, int fromLevel, int toLevel, boolean chooseNone, List list) {
		// set list title
		list.setTitle(StrTab.STEP + " " + Utils.toArabicNumerals(step + 1) + StrTab.OF + 
				Utils.toArabicNumerals(insurCount) + ": " + titles[step]);
		
		// add or remove PREV and RESET commands
		if (step == 0)
		{
			list.removeCommand(cmdReset);
			list.removeCommand(cmdPrev);			
		} else {
			list.addCommand(cmdReset);
			list.addCommand(cmdPrev);						
		}
		
		// add or remove NEXT and COMPUTE commands
		if (step < insurCount - 1) {
			list.setSelectCommand(cmdNext);
			// early showing of compute if only one item remains
			if (toLevel == 1)
				list.addCommand(cmdCompute);
			else
				list.removeCommand(cmdCompute);
		}
		else {
			list.removeCommand(cmdNext);
			list.setSelectCommand(cmdCompute);
		}
			
		// add the list items
		list.deleteAll();
		// provide a "none" choice
		if (chooseNone)
			list.append(StrTab.CHOOSE_NONE, null);
		for (int i = fromLevel; i < toLevel; i++) {
			String strItem = StrTab.LEVEL + " " + Utils.toArabicNumerals(i + 1) + ": " + formattedInsur[step][i] + " " + StrTab.RIAL;				
			list.append(strItem, null);
		}
		
		return list;
	}

	private void updateResultForm() {
		// compute sum first
		int sum = 0;
		for (int i = 0; i < insurCount; i++) {
			itmSummary[i].setLabel(Utils.toArabicNumerals(i + 1) + ". " + titles[i] + ": ");
			if (choice[i] > -1) {
				long thisCost = cost[i][choice[i]];
				itmSummary[i].setText(Utils.formatNumber(thisCost) + " " + StrTab.RIAL);
				sum += thisCost;
			} else {
				// skip no choices
				itmSummary[i].setText(StrTab.CHOOSE_NONE);
			}
		}
		// set text of item to sum
		itmTotal.setText(Utils.formatNumber(sum) + " " + StrTab.RIAL);
		
	}

	protected void destroyApp(boolean arg0) throws MIDletStateChangeException {

	}

	protected void pauseApp() {

	}

	protected void startApp() throws MIDletStateChangeException {
	}
	
	// converts list selection index to a level choice
	private int indexToChoice(int index, int step) {
		int retValue = -1;
		
		if (step == 0)
			// index and choice are the same in step 0
			retValue = index;
		
		if (step > 0) {
			if (index == 0)
				// none is selected in steps > 0
				retValue = -1; 
			else if (step == 1)
				// if none is not selected in step 1, then the chosen level must be the same as step 0, as we have only one other choice
				retValue = choice[0];
			else
				// in other steps, the choice is index -1
				retValue = index - 1;
		}
		return retValue;
	}
	
	private int choiceToIndex(int choice, int step)
	{
		int retValue = 0;
		
		if (step == 0)
			retValue = choice;
		if (step > 0) {
			if (choice == -1)
				retValue = 0;
			else if (step == 1)
				retValue = 1;
			else
				retValue = choice + 1;
		}
		return retValue;
	}
	
	public void commandAction(Command cmd, Displayable d) {
		// Previous command
		if (cmd == cmdPrev)
			curStep--;
		
		// Reset command
		if (cmd == cmdReset){
			curStep = 0;
			// reset choices too
			choice[0] = 0;
			for (int i = 1; i < choice.length; i++) {
				choice[i] = -1;
			}
		}
		
		// Next and Compute commands
		if (cmd == cmdNext || cmd == cmdCompute) {
			// save the current selection and go to next step
			choice[curStep] = indexToChoice(lstChoices.getSelectedIndex(), curStep);
			curStep++;						
		}
		
		// update UI
		if (cmd == cmdNext || cmd == cmdPrev || cmd == cmdReset) {
			// switch buffer
			curBuffer = 1 - curBuffer;
			// levels to be shown from
			int toLevel, fromLevel;
			if (curStep == 1)
				fromLevel = choice[0];
			else
				fromLevel = 0;
			// levels to be shown up to
			if (curStep == 0)
				toLevel = levelCount;
			else
				toLevel = choice[0] + 1;
			// provide a "choose none" for steps > 0
			boolean chooseNone = false;
			if (curStep > 0)
				chooseNone = true;
			// regenerate the list to be shown
			lstChoices = populateList(curStep, fromLevel, toLevel, chooseNone, lstBuffer[curBuffer]);
			lstChoices.setSelectedIndex(choiceToIndex(choice[curStep], curStep), true);
			// display it
			display.setCurrent(lstChoices);
		}
		
		// Update the result screen and show it
		if (cmd == cmdCompute)
		{
			updateResultForm();
			display.setCurrent(frmResult);
		}
		
		// Exit
		if (cmd == cmdExit) {
			notifyDestroyed();
		}
		
		// About
		if (cmd == cmdAbout) {
			display.setCurrent(alertAbout);
		}
		
		// Comments
		if (cmd == cmdComments) {
			Displayable cur = display.getCurrent();
			alertComments.setString(comments[curStep]);
			display.setCurrent(alertComments, cur);
		}
	}
}
