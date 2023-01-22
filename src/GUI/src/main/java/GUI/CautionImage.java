package GUI;

import javafx.scene.control.TableCell;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class CautionImage extends TableCell<Blower, String> {
    private final ImageView imageView = new ImageView();
    @Override
    protected void updateItem(String url, boolean empty) {
        super.updateItem(url, empty);

        imageView.setImage(new Image(Objects.requireNonNull(GUI.class.getResourceAsStream("caution.png"))));
        imageView.setFitHeight(25);
        imageView.setFitWidth(25);
        setGraphic(imageView);
    }

}