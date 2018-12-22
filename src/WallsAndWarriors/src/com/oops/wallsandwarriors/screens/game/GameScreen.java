package com.oops.wallsandwarriors.screens.game;

import com.oops.wallsandwarriors.Game;
import com.oops.wallsandwarriors.GameConstants;
import com.oops.wallsandwarriors.SolutionManager;
import com.oops.wallsandwarriors.StorageManager;
import com.oops.wallsandwarriors.model.ChallengeData;
import com.oops.wallsandwarriors.model.HighTowerData;
import com.oops.wallsandwarriors.model.KnightData;
import com.oops.wallsandwarriors.model.WallData;
import com.oops.wallsandwarriors.screens.Screen;
import com.oops.wallsandwarriors.screens.challenges.CampaignChallengesData;
import com.oops.wallsandwarriors.util.EncodeUtils;
import com.oops.wallsandwarriors.view.BackgroundView;
import com.oops.wallsandwarriors.view.BoundedViewObject;
import com.oops.wallsandwarriors.view.GridView;
import com.oops.wallsandwarriors.view.HighTowerView;
import com.oops.wallsandwarriors.view.KnightView;
import com.oops.wallsandwarriors.view.GamePaletteView;
import com.oops.wallsandwarriors.view.WallView;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Group;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.input.MouseButton;

/**
 * This class defines the structure of the game screen (for both custom challenges and
 * campaign challenges)
 * Extends the BaseGameScreen .
 * @author OOPs
 */
public class GameScreen extends BaseGameScreen {

    private GamePaletteView wallPaletteView;
    private Screen previousScreen;
    private Button muteButton;

    /**
     * A method to set the previous screen of the current screen.
     * @param previousScreen as the given previous Screen.
     */

    public void setPreviousScreen(Screen previousScreen) {
        this.previousScreen = previousScreen;
    }

    @Override
    protected void initViewObjects() {
        super.initViewObjects();
        gridView = new GridView(GameConstants.GRID_X, GameConstants.GRID_Y,
                GameConstants.GRID_MARGIN, GameConstants.GRID_B);
        ChallengeData challenge = Game.getInstance().challengeManager.getChallengeData();
        for (WallData wall : challenge.walls) {
            wallViews.add(new WallView(wall));
        }
        for (KnightData knight : challenge.knights) {
            knightViews.add(new KnightView(knight));
        }
        for (HighTowerData highTower : challenge.highTowers) {
            highTowerViews.add(new HighTowerView(highTower));
        }
        backgroundView = new BackgroundView(false);
        wallPaletteView = new GamePaletteView();
        clickables.addAll(wallViews);
    }

    @Override
    protected void addComponents(Group root) {
        addButton(root, "Back", GameConstants.GAME_SCR_BACK_X, GameConstants.GAME_SCR_BACK_Y, new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                changeScreen(previousScreen);
            }
        });
        addButton(root, "Hint", GameConstants.GAME_SCR_HINT_X, GameConstants.GAME_SCR_HINT_Y, new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                showHint();
            }
        });
        addButton(root, "Reset", GameConstants.GAME_SCR_RESET_X, GameConstants.GAME_SCR_RESET_Y, new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                resetState();
            }
        });

        muteButton = addButton(root, "Mute", GameConstants.GAME_SCR_MUTE_X, GameConstants.GAME_SCR_MUTE_Y, new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                if (Game.getInstance().soundManager.soundCheck()){
                    Game.getInstance().soundManager.mute();
                    muteButton.setText("Unmute");
                }
                else {
                    Game.getInstance().soundManager.setInitialVolume();
                    muteButton.setText("Mute");
                }
            }
        });
    }

    @Override
    protected boolean attemptPlacement() {
        if (hoveredBlock != null && selectedPiece != null) {
            Game.getInstance().soundManager.playPrimary();
        } 
        if (hoveredBlock != null && selectedPiece != null &&
            Game.getInstance().gridManager.attemptPlacement(hoveredBlock, selectedPiece)) {
            saveSession();
            checkSolution(false);
            return true;
        }
        return false;
    }

    @Override
    protected boolean handleViewClick(BoundedViewObject clickedView, MouseButton button) {
        if (selectedPiece == null) {
            Game.getInstance().soundManager.playSecondary();
            if (clickedView instanceof WallView) {
                WallView wallView = (WallView) clickedView;
                WallData clickedWall = wallView.getModel();
                if (button == MouseButton.PRIMARY) {
                    if (selectedPiece == clickedWall) {
                        selectedPiece = null;
                    } else {
                        clickedWall.setPosition(null);
                        selectedPiece = clickedWall;
                    }
                    return true;
                } else if (button == MouseButton.SECONDARY) {
                    clickedWall.setPosition(null);
                    saveSession();
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    protected void resetState() {
        Game.getInstance().soundManager.playSecondary();
        selectedPiece = null;
        Game.getInstance().challengeManager.getChallengeData().resetWalls();
    }

    @Override
    protected void step(double deltaTime) {
        backgroundView.draw(graphics, deltaTime);
        fpsDisplayView.draw(graphics, deltaTime);
        wallPaletteView.draw(graphics, deltaTime);
        gridView.draw(graphics, deltaTime);

        drawKnights(deltaTime);
        drawHighTowers(deltaTime);
        drawWalls(deltaTime);
    }

    @Override
    protected void drawWalls(double deltaTime) {
        WallView selectedWallView = null;
        for (WallView wallView : wallViews) {
            double dragX;
            double dragY;
            boolean previewSuitable;
            if (hoveredBlock == null) {
                dragX = lastMouseX;
                dragY = lastMouseY;
                previewSuitable = false;
            } else {
                dragX = gridView.translateToScreenX(hoveredBlock.x + 0.5);
                dragY = gridView.translateToScreenY(hoveredBlock.y + 0.5);
                previewSuitable = placementIsSuitable;
            }
            wallView.update(selectedPiece == wallView.getModel(), previewSuitable, dragX, dragY);
            if (selectedPiece == wallView.getModel()) {
                selectedWallView = wallView;
            } else {
                wallView.draw(graphics, deltaTime);
            }
        }
        if (selectedWallView != null) {
            selectedWallView.draw(graphics, deltaTime);
        }
    }

    private void checkSolution(boolean showMistake) {
        ChallengeData challenge = Game.getInstance().challengeManager.getChallengeData();
        SolutionManager solutionManager = Game.getInstance().solutionManager;
        ArrayList<KnightData> incorrectRedKnights = solutionManager.checkSolution(challenge);
        if (incorrectRedKnights == null) {
            handleAlert("Mistake", "Walls are not closed.", showMistake);
        } else if (!incorrectRedKnights.isEmpty()) {
            handleAlert("Mistake", "Problem with red Knights: " +
                    incorrectRedKnights.size(), showMistake);
        } else {
            Game.getInstance().soundManager.playCongrats();
            handleAlert("WIN", "Congratulations! You solved the challenge.", true);
            if (previousScreen == Game.getInstance().screenManager.campaignChallenges) {
                editProgressInfo(challenge, true);
            }
        }
    }
    private void showHint(){
            ChallengeData challengeData = Game.getInstance().challengeManager.getChallengeData();
            ChallengeData solutionData = Game.getInstance().hintManager.getChallengeData();

            challengeData.walls.get(1).setWallDefinition(solutionData.walls.
                    get(1).getWallDefinition());

            boolean placable =  Game.getInstance().gridManager.isPiecePlacable(solutionData.walls.
                    get(1).getPosition(), challengeData.walls.get(1));
            if (!placable) {
                resetState();
            }

            Game.getInstance().gridManager.attemptPlacement(solutionData.walls.
                    get(1).getPosition(),challengeData.walls.get(1));
            saveSession();
            checkSolution(false);
    }

    private void handleAlert(String title, String content, boolean show) {
        if (show) {
            ButtonType stayType = new ButtonType("Stay Here", ButtonBar.ButtonData.CANCEL_CLOSE);
            ButtonType backType = new ButtonType("Go Back");
            ButtonType nextType = new ButtonType("Go to the Next Challenge");
            
            Alert alert = new Alert(Alert.AlertType.NONE);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(content);
            alert.getButtonTypes().add(stayType);
            alert.getButtonTypes().add(backType);
            if (previousScreen == Game.getInstance().screenManager.campaignChallenges) {
                alert.getButtonTypes().add(nextType);
            }
            
            Optional<ButtonType> result = alert.showAndWait();

            if (result.get() == nextType) {
               goNextChallenge();
            } else if (result.get() == backType) {
               changeScreen(previousScreen);
            }
        }
    }
    
    private void changeScreen(Screen screen) {
        Game.getInstance().storageManager.clearSessionFile();
        Game.getInstance().setScreen(screen);
    }
    
    private void saveSession() {
        try {
            ChallengeData challengeData = Game.getInstance().challengeManager.getChallengeData();
            ChallengeData hintData = Game.getInstance().hintManager.getChallengeData();
            StorageManager storageManager = Game.getInstance().storageManager;
            File inputFile = storageManager.sessionData;
            File tempFile = new File(storageManager.wnwData, "tempSession.dat");

            BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(tempFile));

            String challengeCode = EncodeUtils.encode(challengeData);
            String hintCode = EncodeUtils.encode(hintData);
            bufferedWriter.write(challengeCode + "\n" + hintCode + "\n");
            bufferedWriter.close();
            if (!inputFile.delete()) {
                System.out.println("Could not delete file");
                return;
            }

            //Rename the new file to the filename the original file had.
            if (!tempFile.renameTo(inputFile)) {
                System.out.println("Could not rename file");
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void editProgressInfo(ChallengeData challengeData, boolean isSolved)
    {
        FileWriter fileWriter;
        StorageManager storageManager = Game.getInstance().storageManager;

        try {
            fileWriter = new FileWriter(storageManager.progressData);

            int index = getIndex(challengeData);
            if (isSolved) {
                if (index < CampaignChallengesData.campaignChallengesProgress.size() - 1) {
                    CampaignChallengesData.campaignChallengesProgress.set(index + 1, "1");
                }
            }
            else {
                CampaignChallengesData.campaignChallengesProgress.set(index + 1, "0");
            }

            fileWriter.write(CampaignChallengesData.campaignChallengesProgress.toString() + "\n");
            fileWriter.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private int getIndex(ChallengeData challengeData)
    {
        for (int i = 0; i < CampaignChallengesData.campaignChallenges.size(); i++) {
            if (challengeData.getName().equals(CampaignChallengesData.campaignChallenges.get(i).getName())) {
                return i;
            }
        }
        return -1;
    }
    
    private void goNextChallenge() {
        ChallengeData current = Game.getInstance().challengeManager.getChallengeData();
        List<String> progress = CampaignChallengesData.campaignChallengesProgress;
        int nextIndex = getIndex(current) + 1;
        if (nextIndex == 0 || nextIndex >= progress.size()) {
            changeScreen(previousScreen);
        } else {
            ChallengeData next = CampaignChallengesData.campaignChallenges.get(nextIndex);
            Game.getInstance().challengeManager.setChallengeData(next.createCopy(true));
            Game.getInstance().hintManager.setChallengeData(next.createCopy(false));
            Game.getInstance().setScreen(this);
        }
        
    }
    
}
