
    private static void loadAnimations() {
        System.out.println("Loading animations from resources...");
        try {
            URI uri = Main.class.getResource("/animations").toURI();
            Path myPath;
            if (uri.getScheme().equals("jar")) {
                try (FileSystem fs = FileSystems.newFileSystem(uri, Collections.emptyMap())) {
                    myPath = fs.getPath("/animations");
                    processAnimationPath(myPath);
                }
            } else {
                myPath = Paths.get(uri);
                processAnimationPath(myPath);
            }
        } catch (Exception e) {
            System.err.println("Failed to load animations from resources directory.");
            e.printStackTrace();
        }
    }
    // processing the animation path to find and parse animation files
    private static void processAnimationPath(Path path) {
        try (Stream<Path> walk = Files.walk(path, 1)) {
            List<Path> files = walk.filter(p -> p.toString().endsWith(".animation.json")).collect(Collectors.toList());
            if (files.isEmpty()) {
                System.err.println("No .animation.json animation files found in " + path);
                return;
            }
            for (Path p : files) {
                try (InputStream is = Files.newInputStream(p)) {
                    JsonObject json = JsonParser.parseReader(new InputStreamReader(is)).getAsJsonObject();
                    parseBlockbenchAnimations(json);
                } catch (Exception e) {
                    System.err.println("Failed to parse animation file: " + p.getFileName());
                    e.printStackTrace();
                }
            }
        } catch (Exception e) {
            System.err.println("Could not walk animation path: " + path);
            e.printStackTrace();
        }
    }
    // parsing animations from blockbench JSON format
    private static void parseBlockbenchAnimations(JsonObject json) {
        if (!json.has("animations")) {
            System.err.println("Animation file missing 'animations' section");
            return;
        }

        JsonObject animationsObj = json.getAsJsonObject("animations");
        for (Map.Entry<String, JsonElement> entry : animationsObj.entrySet()) {
            String animName = entry.getKey();
            JsonObject animData = entry.getValue().getAsJsonObject();

            double length = animData.has("animation_length") ? animData.get("animation_length").getAsDouble() : 1.0;
            boolean loop = animData.has("loop") ? animData.get("loop").getAsBoolean() : true;

            Map<String, BoneAnimation> boneAnimations = new HashMap<>();

            if (animData.has("bones")) {
                JsonObject bones = animData.getAsJsonObject("bones");
                for (Map.Entry<String, JsonElement> boneEntry : bones.entrySet()) {
                    String boneName = boneEntry.getKey();
                    JsonObject boneData = boneEntry.getValue().getAsJsonObject();

                    Map<Double, Vec> rotationKeyframes = parseKeyframes(boneData, "rotation");
                    Map<Double, Vec> positionKeyframes = parseKeyframes(boneData, "position");
                    Map<Double, Vec> scaleKeyframes = parseKeyframes(boneData, "scale");

                    boneAnimations.put(boneName, new BoneAnimation(rotationKeyframes, positionKeyframes, scaleKeyframes));
                }
            }

            animations.put(animName, new BlockbenchAnimation(animName, length, loop, boneAnimations));
            System.out.println("Loaded animation: " + animName);
        }
    }
    // parsing keyframes from blockbench JSON format
    private static Map<Double, Vec> parseKeyframes(JsonObject boneData, String type) {
        Map<Double, Vec> keyframes = new LinkedHashMap<>();

        if (boneData.has(type)) {
            JsonElement typeElement = boneData.get(type);
            if(typeElement.isJsonObject()){
                JsonObject keyframeData = typeElement.getAsJsonObject();
                for (Map.Entry<String, JsonElement> entry : keyframeData.entrySet()) {
                    try {
                        double time = Double.parseDouble(entry.getKey());
                        JsonElement value = entry.getValue();
                        Vec vec = parseVec(value);
                        keyframes.put(time, vec);
                    } catch (NumberFormatException e) {
                        System.err.println("Invalid time format in keyframe: " + entry.getKey());
                    }
                }
            }
        }
        return keyframes;
    }
    // parsing vectors from blockbench JSON format
    private static Vec parseVec(JsonElement value) {
        if (value.isJsonObject()) {
            JsonObject obj = value.getAsJsonObject();
            if (obj.has("post")) {
                var post = obj.getAsJsonArray("post");
                return new Vec(
                        post.get(0).getAsDouble(),
                        post.get(1).getAsDouble(),
                        post.get(2).getAsDouble()
                );
            }
        } else if (value.isJsonArray()) {
            var arr = value.getAsJsonArray();
            return new Vec(
                    arr.get(0).getAsDouble(),
                    arr.get(1).getAsDouble(),
                    arr.get(2).getAsDouble()
            );
        }
        return Vec.ZERO;
    }
