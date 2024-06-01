package com.serhat.realtimeaidrawing.model;

import com.serhat.realtimeaidrawing.Utils;

import java.io.File;

public class GalleryModel {

    public String prompt;
    public String negative_prompt;
    public int width;
    public int height;
    public String path;
    public long id;
    public String seed;
    public String addedDate;
    public boolean isSelected = false;

    public GalleryModel() {

    }


    public GalleryModel(long id, String prompt, String negative_prompt,
                        int width, int height, String path, String seed, String addedDate) {
        this.id = id;
        this.prompt = prompt;
        this.negative_prompt = negative_prompt;
        this.width = width;
        this.height = height;
        this.path = path;
        this.seed = seed;
        this.addedDate = addedDate;
    }

    public void deleteFromDisk() {
        try {
            File file = new File(path);
            if (file.exists()) {
                file.delete();
            }
        } catch (Exception e) {
            Utils.e("GalleryModel", e.getMessage());
        }
    }

}
