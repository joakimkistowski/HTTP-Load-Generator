/**
 * Copyright 2017 Joakim von Kistowski
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package tools.descartes.dlim.httpscripttester;

import java.io.File;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.RowConstraints;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.web.WebView;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.stage.Stage;

/**
 * Graphical window for testing LUA scripts.
 * @author Joakim von Kistowski
 *
 */
public class ScriptTestWindow extends Application {
	
	private static String scriptPath = "";
	private int callNum = 0;
	private String call = "";
	
	private Label callNumLabel;
	private TextField callTextField;
	private GridPane grid;
	private WebView webView;
	private Button nextBtn;
	
	private ScriptKeeper scriptKeeper;
	
	/**
	 * Run the tester.
	 * @param args No args needed.
	 */
	public static void main(String[] args) {
		File scriptFile = null;
		if (args.length > 0 && args[0].trim().length() > 0) {
			scriptFile = new File(args[0].trim());
		}
		if (scriptFile != null && scriptFile.exists()) {
			scriptPath = scriptFile.getAbsolutePath();
		} else {
			scriptPath = "";
		}
		
		ScriptTestWindow.launch();
	}
	
	@Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Lua Script Tester for Fakehost_http");
        getScriptPath(primaryStage);
        
        if (!scriptPath.isEmpty()) {
        	scriptKeeper = new ScriptKeeper(scriptPath);
            
            initializeGrid();
            addControls();
            
            primaryStage.setScene(new Scene(grid, IConstants.DEFAULT_WINDOW_WIDTH, IConstants.DEFAULT_WINDOW_HEIGHT));
            primaryStage.show();
        }
    }

	private void getScriptPath(Stage primaryStage) {
		if (scriptPath.isEmpty()) {
			FileChooser fileChooser = new FileChooser();
			 fileChooser.setTitle("Choose Script");
			 fileChooser.getExtensionFilters().addAll(
			         new ExtensionFilter("Lua Scripts", "*.lua"),
			         new ExtensionFilter("All Files", "*.*"));
			 File selectedFile = fileChooser.showOpenDialog(primaryStage);
			 if (selectedFile != null) {
				 scriptPath = selectedFile.getAbsolutePath();
			 }
		}
	}
	
	private void initializeGrid() {
		grid = new GridPane();
		//grid.setAlignment(Pos.CENTER);
		grid.setHgap(10);
		grid.setVgap(10);
		grid.setPadding(new Insets(25, 25, 25, 25));
		grid.setMaxSize(Region.USE_COMPUTED_SIZE, Region.USE_COMPUTED_SIZE);
		ColumnConstraints c1 = new ColumnConstraints();
		c1.setFillWidth(true);
		c1.setHgrow(Priority.ALWAYS);
		grid.getColumnConstraints().addAll(new ColumnConstraints(), c1, new ColumnConstraints());
		RowConstraints r2 = new RowConstraints();
		r2.setFillHeight(true);
		r2.setVgrow(Priority.ALWAYS);
	}
	
	private void addControls() {
		Text scriptText = new Text("Script = \"" + scriptPath + "\"");
		scriptText.setFont(Font.font("Arial", FontWeight.NORMAL, 14));
		grid.add(scriptText, 0, 0, 3, 1); 
		callNumLabel = new Label();
		setCallNum(callNum);
		grid.add(callNumLabel, 0, 1);
	
		callTextField = new TextField();
		grid.add(callTextField, 1, 1);
		callTextField.setEditable(false);
		setCall(call);
		GridPane.setFillWidth(callTextField, true);
		
		nextBtn = new Button();
		nextBtn.setText("Next Call");
		nextBtn.setOnAction((event) -> {
                scriptKeeper.execute();
                setCall(scriptKeeper.getCall());
                setCallNum(scriptKeeper.getLastCallNum());
                webView.getEngine().loadContent(scriptKeeper.getLastHTTPResponse());
            }
        );
		grid.add(nextBtn, 2, 1);
		
		webView = new WebView();
		GridPane.setFillHeight(webView, true);
		GridPane.setVgrow(webView, Priority.ALWAYS);
		grid.add(webView, 0, 2, 3, 1); 
		
		webView.getEngine().load("http://google.com");
	}
	
	private void setCallNum(int callNum) {
		this.callNum = callNum;
		callNumLabel.setText("Call #" + callNum + " = ");
	}
	
	private void setCall(String call) {
		this.call = call;
		callTextField.setText(call);
	}
}
