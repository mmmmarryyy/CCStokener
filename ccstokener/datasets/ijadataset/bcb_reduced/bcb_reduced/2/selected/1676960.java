package rma.flp;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URL;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import com.google.appengine.api.users.UserService;

@Controller
public class ImportPictureController {

    PictureRepository pictureRepository;

    ImageTouchupService imageFilterService;

    UserService userService;

    @Autowired
    public ImportPictureController(PictureRepository pictureRepository, ImageTouchupService imagesService, UserService userService) {
        this.pictureRepository = pictureRepository;
        this.imageFilterService = imagesService;
        this.userService = userService;
    }

    @RequestMapping("/import")
    public String importPicture(@ModelAttribute PictureImportCommand command) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        URL url = command.getUrl();
        IOUtils.copy(url.openStream(), baos);
        byte[] imageData = imageFilterService.touchupImage(baos.toByteArray());
        String filename = StringUtils.substringAfterLast(url.getPath(), "/");
        String email = userService.getCurrentUser().getEmail();
        Picture picture = new Picture(email, filename, command.getDescription(), imageData);
        pictureRepository.store(picture);
        return "redirect:/picture/gallery";
    }
}
