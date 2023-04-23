package glorydark.customform.forms;

import cn.nukkit.Player;
import cn.nukkit.form.window.FormWindow;
import cn.nukkit.form.window.FormWindowCustom;
import cn.nukkit.form.window.FormWindowModal;
import cn.nukkit.form.window.FormWindowSimple;
import cn.nukkit.network.protocol.ModalFormRequestPacket;
import glorydark.customform.CustomFormMain;
import glorydark.customform.scriptForms.data.SoundData;
import glorydark.customform.scriptForms.data.execute_data.ResponseExecuteData;
import glorydark.customform.scriptForms.data.execute_data.SimpleResponseExecuteData;
import glorydark.customform.scriptForms.data.execute_data.StepResponseExecuteData;
import glorydark.customform.scriptForms.data.execute_data.ToggleResponseExecuteData;
import glorydark.customform.scriptForms.data.requirement.economy.EconomyRequirementData;
import glorydark.customform.scriptForms.data.requirement.economy.EconomyRequirementType;
import glorydark.customform.scriptForms.data.requirement.Requirements;
import glorydark.customform.scriptForms.data.requirement.tips.TipsRequirementData;
import glorydark.customform.scriptForms.data.requirement.tips.TipsRequirementType;
import glorydark.customform.scriptForms.form.ScriptForm;
import glorydark.customform.scriptForms.form.ScriptFormCustom;
import glorydark.customform.scriptForms.form.ScriptFormModal;
import glorydark.customform.scriptForms.form.ScriptFormSimple;
import lombok.Data;

import java.util.*;

public class FormCreator {
    public static final LinkedHashMap<String, WindowInfo> UI_CACHE = new LinkedHashMap<>();

    // Stored information and configuration about loaded forms.
    public static LinkedHashMap<String, ScriptForm> formScripts = new LinkedHashMap<>();

    // This value effectively reduces the conflicts brought by the duplication of ID value inside the Player.class(Nukkit)
    public static int formId = -1;

    /*
        WindowInfo stores the information, including its type and script name.
    */
    @Data
    public static class WindowInfo{
        private FormType type;

        private String script;

        public WindowInfo(FormType type, String script){
            this.type = type;
            this.script = script;
        }
    }

    /*
        Basic Refined Method.
        Modified from MurderMystery sourcecode one or two year ago.
        Especially thanks to lt-name(LT-name)!
    */
    public static void showFormToPlayer(Player player, FormType formType, String identifier) {
        if(player.namedTag.contains("lastFormRequestMillis") && System.currentTimeMillis() - player.namedTag.getLong("lastFormRequestMillis") < CustomFormMain.coolDownMillis) {
            player.sendMessage(CustomFormMain.language.translateString(player, "operation_so_fast"));
            return;
        }
        FormWindow window = formScripts.get(identifier).getWindow(player);
        ModalFormRequestPacket packet = new ModalFormRequestPacket();
        packet.formId = formId;
        packet.data = window.getJSONData();
        player.dataPacket(packet);
        player.namedTag.putLong("lastFormRequestMillis", System.currentTimeMillis());
        UI_CACHE.put(player.getName(), new WindowInfo(formType, identifier));
    }

    /*
        By this function, you can show a certain form whose identifier is the same as identifier.
    */
    public static void showScriptForm(Player player, String identifier){
        if(formScripts.containsKey(identifier)){
            ScriptForm script = formScripts.get(identifier);
            FormWindow window = script.getWindow(player);
            if(script.getOpenSound() != null){
                script.getOpenSound().addSound(player);
            }
            if(window instanceof FormWindowSimple) {
                showFormToPlayer(player, FormType.ScriptSimple, identifier);
            }
            if(window instanceof FormWindowModal) {
                showFormToPlayer(player, FormType.ScriptModal, identifier);
            }
            if(window instanceof FormWindowCustom) {
                showFormToPlayer(player, FormType.ScriptCustom, identifier);
            }
        }
    }

    /*
        By this function, you can build up a condition set.
        A condition set consists of Tips_ConditionData and ConditionData.
        You can see some provided method inside the Conditions.class.

        * If you want to add a new type of condition,
        please make some tiny modifications inside the Conditions.class.
    */
    public static Requirements buildRequirements(List<Map<String, Object>> conditionList, boolean chargeable){
        Requirements requirements = new Requirements(new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), chargeable);
        for(Map<String, Object> map: conditionList){
            String type = (String) map.get("type");
            EconomyRequirementData data = null;
            TipsRequirementData tips_data = null;
            switch (type){
                case "EconomyAPI":
                    // This is the way we deal with EconomyAPI-type condition
                    data = new EconomyRequirementData(null, 0d, new Object());
                    data.setType(EconomyRequirementType.EconomyAPI);
                    data.setAmount(Double.parseDouble(map.get("cost").toString()));
                    break;
                case "Points":
                    // This is the way we deal with Points-type condition
                    data = new EconomyRequirementData(null, 0d, new Object());
                    data.setType(EconomyRequirementType.Points);
                    data.setAmount(Double.parseDouble(map.get("cost").toString()));
                    break;
                case "DCurrency":
                    // This is the way we deal with DCurrency-type condition
                    data = new EconomyRequirementData(null, 0d, new Object());
                    data.setType(EconomyRequirementType.DCurrency);
                    data.setAmount(Double.parseDouble(map.get("cost").toString()));
                    data.setExtraData(new String[]{(String) map.get("currencyType")});
                    break;
                case "Tips":
                    // This is the way we deal with Tips-type condition
                    String identifier = (String) map.get("identifier");
                    String comparedSign = (String) map.get("compared_sign");
                    Object comparedValue = map.get("compared_value");
                    String displayName = (String) map.get("display_name");
                    List<String> failed_messages = (List<String>) map.getOrDefault("failed_messages", new ArrayList<>());
                    switch (comparedSign){
                        case ">":
                            tips_data = new TipsRequirementData(TipsRequirementType.Bigger, identifier, comparedValue, displayName, failed_messages);
                            break;
                        case ">=":
                            tips_data = new TipsRequirementData(TipsRequirementType.BiggerOrEqual, identifier, comparedValue, displayName, failed_messages);
                            break;
                        case "=":
                            tips_data = new TipsRequirementData(TipsRequirementType.Equal, identifier, comparedValue, displayName, failed_messages);
                            break;
                        case "<":
                            tips_data = new TipsRequirementData(TipsRequirementType.Smaller, identifier, comparedValue, displayName, failed_messages);
                            break;
                        case "<=":
                            tips_data = new TipsRequirementData(TipsRequirementType.SmallerOrEqual, identifier, comparedValue, displayName, failed_messages);
                            break;
                    }
                    break;
                case "extraData":
                    requirements.setCommands((List<String>) map.getOrDefault("commands", new ArrayList<>()));
                    requirements.setMessages((List<String>) map.getOrDefault("messages", new ArrayList<>()));
                    requirements.setFailedCommands((List<String>) map.getOrDefault("failed_commands", new ArrayList<>()));
                    requirements.setFailedMessages((List<String>) map.getOrDefault("failed_messages", new ArrayList<>()));
                    break;
            }
            if(data != null){
                requirements.addCondition(data);
            }
            if(tips_data != null){
                requirements.addTipsCondition(tips_data);
            }
        }
        return requirements;
    }

    /*
        This is how we preload our form info.
        And we identify the type of the form by the Integer.
        0: simple  1: custom  2: modal
     */
    public static boolean loadForm(String identifier, Map<String, Object> config){
        switch ((int) config.get("type")){
            case 0:
                //simple
                List<SimpleResponseExecuteData> simpleResponseExecuteDataList = new ArrayList<>();
                if(config.containsKey("components")) {
                    for (Map<String, Object> component : (List<Map<String, Object>>) config.getOrDefault("components", new ArrayList<>())) {
                        SimpleResponseExecuteData data = new SimpleResponseExecuteData((List<String>) component.getOrDefault("commands", new ArrayList<>()), (List<String>) component.getOrDefault("messages", new ArrayList<>()), (List<String>) component.getOrDefault("failed_commands", new ArrayList<>()), (List<String>) component.getOrDefault("failed_messages", new ArrayList<>()));
                        if(component.containsKey("conditions")) {
                            List<Requirements> conditions = new ArrayList<>();
                            Map<String, Object> conditionData = (Map<String, Object>) component.get("conditions");
                            for(List<Map<String, Object>> object: (List<List<Map<String, Object>>>)conditionData.get("data")){
                                conditions.add(buildRequirements(object, (Boolean) conditionData.getOrDefault("chargeable", true)));
                            }
                            data.setRequirements(conditions);
                        }
                        simpleResponseExecuteDataList.add(data);
                    }
                }
                ScriptFormSimple simple = new ScriptFormSimple(config, simpleResponseExecuteDataList, new SoundData("", 1f, 0f, true));
                if(config.containsKey("open_sound")) {
                    Map<String, Object> openSoundMap = (Map<String, Object>) config.get("open_sound");
                    simple.setOpenSound(new SoundData((String) openSoundMap.get("name"), Float.parseFloat(openSoundMap.getOrDefault("volume", 1f).toString()), Float.parseFloat(openSoundMap.getOrDefault("pitch", 0f).toString()), (Boolean) openSoundMap.getOrDefault("personal", true)));
                }
                if (simple.getWindow() != null) {
                    formScripts.put(identifier, simple);
                    return true;
                }
                break;
            case 1:
                List<ResponseExecuteData> out = new ArrayList<>();
                //custom
                for (Map<String, Object> component : (List<Map<String, Object>>) config.getOrDefault("components", new ArrayList<>())) {
                    String type = (String) component.getOrDefault("type", "");
                    if (type.equals("StepSlider") || type.equals("Dropdown")) {
                        List<SimpleResponseExecuteData> data = new ArrayList<>();
                        List<Map<String, Object>> maps = (List<Map<String, Object>>) component.getOrDefault("responses", new LinkedHashMap<>());
                        for (Map<String, Object> map : maps) {
                            data.add(new SimpleResponseExecuteData((List<String>) map.getOrDefault("commands", new ArrayList<>()), (List<String>) map.getOrDefault("messages", new ArrayList<>()), new ArrayList<>(), new ArrayList<>()));
                        }
                        out.add(new StepResponseExecuteData(data));
                    } else {
                        if (type.equals("Toggle")) {
                            Map<String, Object> maps = (Map<String, Object>) component.getOrDefault("responses", new LinkedHashMap<>());
                            out.add(new ToggleResponseExecuteData((List<String>) maps.get("true_commands"), (List<String>) maps.get("true_messages"), (List<String>) maps.get("false_commands"), (List<String>) maps.get("false_messages")));
                        } else {
                            SimpleResponseExecuteData data = new SimpleResponseExecuteData((List<String>) component.getOrDefault("commands", new ArrayList<>()), (List<String>) component.getOrDefault("messages", new ArrayList<>()), (List<String>) component.getOrDefault("failed_commands", new ArrayList<>()), (List<String>) component.getOrDefault("failed_messages", new ArrayList<>()));
                            if(component.containsKey("conditions")) {
                                List<Requirements> requirements = new ArrayList<>();
                                Map<String, Object> conditionData = (Map<String, Object>) component.get("conditions");
                                for(List<Map<String, Object>> object: (List<List<Map<String, Object>>>)conditionData.get("data")){
                                    requirements.add(buildRequirements(object, (Boolean) conditionData.getOrDefault("chargeable", true)));
                                }
                                data.setRequirements(requirements);
                            }
                            out.add(data);
                        }
                    }
                }
                ScriptFormCustom custom = new ScriptFormCustom(config, out, new SoundData("", 1f, 0f, true));
                if(config.containsKey("open_sound")) {
                    Map<String, Object> openSoundMap = (Map<String, Object>) config.get("open_sound");
                    custom.setOpenSound(new SoundData((String) openSoundMap.get("name"), Float.parseFloat(openSoundMap.getOrDefault("volume", 1f).toString()), Float.parseFloat(openSoundMap.getOrDefault("pitch", 0f).toString()), (Boolean) openSoundMap.getOrDefault("personal", true)));
                }
                if(custom.getWindow() != null){
                    formScripts.put(identifier, custom);
                    return true;
                }
                break;
            case 2:
                //modal
                simpleResponseExecuteDataList = new ArrayList<>();
                for (Map<String, Object> component : (List<Map<String, Object>>) config.getOrDefault("components", new ArrayList<>())) {
                    SimpleResponseExecuteData data = new SimpleResponseExecuteData((List<String>) component.getOrDefault("commands", new ArrayList<>()), (List<String>) component.getOrDefault("messages", new ArrayList<>()), new ArrayList<>(), new ArrayList<>());
                    simpleResponseExecuteDataList.add(data);
                }
                ScriptFormModal modal = new ScriptFormModal(config, simpleResponseExecuteDataList, new SoundData("", 1f, 0f, true));
                if(config.containsKey("open_sound")) {
                    Map<String, Object> openSoundMap = (Map<String, Object>) config.get("open_sound");
                    modal.setOpenSound(new SoundData((String) openSoundMap.get("name"), Float.parseFloat(openSoundMap.getOrDefault("volume", 1f).toString()), Float.parseFloat(openSoundMap.getOrDefault("pitch", 0f).toString()), (Boolean) openSoundMap.getOrDefault("personal", true)));
                }
                if (modal.getWindow() != null) {
                    formScripts.put(identifier, modal);
                    return true;
                }
                break;
        }
        return false;
    }
}