package haslog;

public class has_log {
    @Override
    public void onFailure(Exception e) {
        logger.debug(() -> new ParameterizedMessage("failed to delete templates [{}]", request.name()),e);
        listener.onFailure(e);
    }

    private void executeScript(String filePath) {
        InputStream resource = Thread.currentThread().getContextClassLoader().getResourceAsStream(filePath);
        if (resource == null) {
            logger().warn("Could not load classpath init script: {}", filePath);
            throw new ScriptUtils.ScriptLoadException("Could not load classpath init script: " + filePath + ". Resource not found.");
        }
        try (Scanner scanner = new Scanner(resource).useDelimiter(";")) {
            while (scanner.hasNext()) {
                String statement = scanner.next().trim();
                if (statement.isEmpty()) { continue; }
                session.writeTransaction(tx -> {
                    tx.run ( statement );
                    tx.commit();
                    return null;
                });
            }
        }
    }

    public void start() {
        doCacheStartup().finish(() -> {
            LOG.info("Eth1DataManager successfully ran cache startup logic");
            cacheStartupDone.set(true);
            eventBus.register(this);
        });
    }

    public void testMinimumShouldMatch() throws ExecutionException, InterruptedException {
        logger.info("Creating the index ...");
        assertAcked(prepareCreate("test").addMapping("type1", "text", "type=text,analyzer=whitespace").setSettings(Settings.builder().put(SETTING_NUMBER_OF_SHARDS, 1)));
        ensureGreen();
        logger.info("Indexing with each doc having one less term ...");
        List<IndexRequestBuilder> builders = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            String text = "" ;
            for (int j = 1; j <= 10 - i; j++) {
                text += j + " ";
            }
            builders.add(client().prepareIndex("test", "type1",i + "").setSource("text", text));
        }

        indexRandom(true ,builders);
        logger.info("Testing each minimum_should_match from 0% - 100% with 10% increment ...");
        for (int i = 0; i <= 10; i++) {String minimumShouldMatch = (10 * i) + "%";
            MoreLikeThisQueryBuilder mltQuery = moreLikeThisQuery(new String[] { "text" }, new String [] { "1 2 3 4 5 6 7 8 9 10" }, null).minTermFreq(1).minDocFreq(1).minimumShouldMatch(minimumShouldMatch);
            logger.info("Testing with minimum_should_match = {}", minimumShouldMatch);
            SearchResponse response = client().prepareSearch("test").setTypes("type1").setQuery(mltQuery).get();
            assertSearchResponse(response);
            if (minimumShouldMatch.equals("0%")) {
                assertHitCount(response,10);
            } else {
                assertHitCount(response, 11 - i);
            }
        }
    }

    protected CheckSshAnswer execute(CheckSshCommand cmd) {
        String vmName = cmd.getName();
        String privateIp = cmd.getIp();
        int cmdPort = cmd.getPort();
        if(s_logger.isDebugEnabled()) {
            s_logger.debug("Ping command port, " + privateIp + ":" + cmdPort );
        }
        try {
            String result = connect(cmd.getName(), privateIp, cmdPort);
            if(result != null) {
                s_logger.error("Can not ping System vm " + vmName + "due to:" + result);
                return new CheckSshAnswer(cmd, "Can not ping System vm " + vmName + "due to:" + result);
            }
        }
        catch(Exception e) {
            s_logger.error("Can not ping System vm " + vmName + "due to exception");
            return new CheckSshAnswer(cmd, e);
        }
        if(s_logger.isDebugEnabled()) {
            s_logger.debug("Ping command port succeeded for vm " + vmName);
        }
        if(VirtualMachineName.isValidRouterName(vmName)) {
            if(s_logger.isDebugEnabled()) {
                s_logger.debug("Execute network usage setup command on " + vmName);
            }
            networkUsage(privateIp, "create", null);
        }
        return new CheckSshAnswer(cmd);
    }

    private ModDiscoverer identifyMods(List<String> additionalContainers) {
        injectedContainers.addAll(additionalContainers);
        FMLLog.log.debug("Building injected Mod Containers {}", injectedContainers);
        mods.add(minecraft);
        mods.add(new InjectedModContainer(mcp, new File("minecraft.jar")));

        for(String cont : injectedContainers) {
            ModContainer mc;
            try {
                mc = (ModContainer)Class.forName(cont, true, modClassLoader).newInstance();
            } catch ( Exception e ) {
                FMLLog.log.error("A problem occurred instantiating the injected mod container {}", cont, e);
                throw new LoaderException (e);
            }
            mods.add(new InjectedModContainer(mc, mc.getSource()));
        }

        ModDiscoverer discoverer = new ModDiscoverer(); {
            FMLLog.log.debug("Attempting to load mods contained in the minecraft jar file and associated classes");
            discoverer.findClasspathMods(modClassLoader);
            FMLLog.log.debug("Minecraft jar mods loaded successfully");
        }

        List<Artifact> maven_canidates = LibraryManager.flattenLists(minecraftDir);
        List<File> file_canidates = LibraryManager.gatherLegacyCanidates(minecraftDir);

        for (Artifact artifact : maven_canidates) {
            artifact = Repository.resolveAll(artifact);
            if (artifact != null) {
                File target = artifact.getFile();
                if (!file_canidates.contains(target)) file_canidates.add(target);
            }
        }

        for (File mod : file_canidates) {
            if (CoreModManager.getIgnoredMods().contains(mod.getName())) {
                FMLLog.log.trace("Skipping already parsed coremod or tweaker {}", mod.getName());
            } else {
                FMLLog.log.debug("Found a candidate zip or jar file {}", mod.getName());
                discoverer.addCandidate(new ModCandidate(mod, mod, ContainerType.JAR));
            }
        }
        mods.addAll(discoverer.identifyMods());
        identifyDuplicates(mods);
        namedMods = Maps.uniqueIndex(mods, ModContainer::getModId);
        FMLLog.log.info("Forge Mod Loader has identified {} mod{} to load", mods.size(), mods.size() != 1 ? "s" : "");
        return discoverer;
    }
}
