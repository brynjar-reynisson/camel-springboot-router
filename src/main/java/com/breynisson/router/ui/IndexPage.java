package com.breynisson.router.ui;

import com.breynisson.router.digitalme.AddContentRequest;
import com.breynisson.router.digitalme.AddContentResponse;
import com.breynisson.router.digitalme.DigitalMeStorage;
import com.breynisson.router.digitalme.SearchResponse;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.view.RedirectView;
import org.springframework.web.util.HtmlUtils;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;

@RestController
public class IndexPage {

    private final DigitalMeStorage storage;

    public IndexPage(DigitalMeStorage storage) {
        this.storage = storage;
    }

    @GetMapping("/")
    public RedirectView index() throws IOException, URISyntaxException {
        return new RedirectView("/index.html");
    }

    @GetMapping("/search")
    public SearchResponse search(@RequestParam String keywords) {
        return storage.search(keywords);
    }

    @GetMapping("/localFile")
    public String localFile(@RequestParam String filePath) throws IOException {
        String content = Files.readString(Paths.get(filePath));
        content = HtmlUtils.htmlEscape(content);
        return "<html><body><p style='white-space: pre-wrap;'>" +
                content +
                "</p></body></html>";
    }

    @PostMapping(value="/addContent", consumes = "application/json", produces = "application/json")
    public AddContentResponse addContent(@RequestBody AddContentRequest addContentRequest) {
        return storage.addContent(addContentRequest);
    }
}
