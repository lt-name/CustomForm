package glorydark.customform.scriptForms.form;

import cn.nukkit.Player;
import cn.nukkit.form.response.FormResponse;
import cn.nukkit.form.response.FormResponseModal;
import cn.nukkit.form.window.FormWindowModal;
import com.smallaswater.npc.variable.BaseVariable;
import com.smallaswater.npc.variable.BaseVariableV2;
import com.smallaswater.npc.variable.VariableManage;
import glorydark.customform.CustomFormMain;
import glorydark.customform.scriptForms.data.SoundData;
import glorydark.customform.scriptForms.data.execute_data.SimpleResponseExecuteData;
import lombok.Data;
import tip.utils.Api;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Data
public class ScriptFormModal implements ScriptForm {

    private SoundData openSound;
    private List<SimpleResponseExecuteData> data;

    private Map<String, Object> config;

    private FormWindowModal window;

    private long startMillis = -1L;

    private long expiredMillis = -1L;

    public ScriptFormModal(Map<String, Object> config, List<SimpleResponseExecuteData> data, SoundData openSound){
        this.config = config;
        this.data = data;
        this.window = initWindow();
        this.openSound = openSound;
    }

    public void execute(Player player, FormResponse response, Object... params){
        FormResponseModal responseModal = (FormResponseModal) response;
        if(data.size() <= (responseModal.getClickedButtonId())){
            return;
        }
        data.get(responseModal.getClickedButtonId()).execute(player, 0, params);
    }

    public FormWindowModal initWindow(){
        FormWindowModal modal;
        Object object = config.getOrDefault("content", "");
        String content = "";
        if(!object.equals("")) {
            if (object instanceof String) {
                content = (String) object;
            } else if (object instanceof ArrayList) {
                StringBuilder tempStr = new StringBuilder();
                List<String> stringListTemp = (List<String>) object;
                if (stringListTemp.size() > 0) {
                    for (int i = 0; i < stringListTemp.size(); i++) {
                        tempStr.append(stringListTemp.get(i));
                        if (i < stringListTemp.size() - 1) {
                            tempStr.append("\n");
                        }
                    }
                }
                content = tempStr.toString();
            }
        }
        List<Map<String, Object>> buttons = (List<Map<String, Object>>) config.getOrDefault("components", new ArrayList<>());
        if(buttons.size() != 2){
            return null;
        }
        if(content.equals("")) {
            modal = new FormWindowModal((String) config.getOrDefault("title", ""), "", (String) buttons.get(0).get("text"), (String) buttons.get(1).get("text"));
        }else{
            modal = new FormWindowModal((String) config.getOrDefault("title", ""), content, (String) buttons.get(0).get("text"), (String) buttons.get(1).get("text"));
        }

        return modal;
    }

    public FormWindowModal getWindow(Player player){
        if(CustomFormMain.enableTips || CustomFormMain.enableRsNPCX){
            FormWindowModal modal = this.getModifiableWindow();
            modal.setContent(replace(modal.getContent(), player, true));
            modal.setTitle(replace(modal.getTitle(), player));
            return modal;
        }
        return this.getModifiableWindow();
    }

    public FormWindowModal getModifiableWindow(){
        return new FormWindowModal(window.getTitle(), window.getContent(), window.getButton1(), window.getButton2());
    }

    @Override
    public SoundData getOpenSound() {
        return openSound;
    }

    public String replaceBreak(String string){
        return string.replace("\\n", "\n");
    }

    public String replace(String string, Player player) {
        return replace(string, player, false);
    }

    /**
     * Refracted in order to expand the usages easily.
     */
    public String replace(String string, Player player, boolean replaceBreak){
        if(CustomFormMain.enableTips){
            string = Api.strReplace(string, player);
        }
        if(CustomFormMain.enableRsNPCX){
            try {
                Field field1 = VariableManage.class.getDeclaredField("VARIABLE_CLASS");
                field1.setAccessible(true);
                ConcurrentHashMap<String, BaseVariable> v1_classes = (ConcurrentHashMap<String, BaseVariable>) field1.get(new ConcurrentHashMap<>());
                for(BaseVariable v1: v1_classes.values()){
                    string = v1.stringReplace(player, string);
                }
                Field field2 = VariableManage.class.getDeclaredField("VARIABLE_V2_CLASS");
                field2.setAccessible(true);
                ConcurrentHashMap<String, BaseVariableV2> v2_classes = (ConcurrentHashMap<String, BaseVariableV2>) field2.get(new ConcurrentHashMap<>());
                for(BaseVariableV2 v2: v2_classes.values()){
                    string = v2.stringReplace(string);
                }
            } catch (NoSuchFieldException | IllegalAccessException ignored) {

            }
        }
        if(replaceBreak){
            string = replaceBreak(string);
        }
        return string;
    }
}
