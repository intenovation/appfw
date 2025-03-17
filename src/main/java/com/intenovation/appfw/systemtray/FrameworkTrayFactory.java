package com.intenovation.appfw.systemtray;

import com.intenovation.appfw.inversemv.ActionModel;
import com.intenovation.appfw.inversemv.CheckboxModel;
import com.intenovation.appfw.inversemv.Model;
import com.intenovation.appfw.inversemv.ParentModel;
import com.intenovation.appfw.inversemv.ParentView;
import com.intenovation.appfw.inversemv.View;

/**
 * Factory class for creating tray views that integrate with the framework.
 */
public class FrameworkTrayFactory {
    
    /**
     * Create a root view for the system tray.
     * 
     * @param model The root model
     * @return A parent view for the system tray
     */
    public static ParentView createRootTrayView(ParentModel model) {
        return new FrameworkParentTrayView(model);
    }
    
    /**
     * Create an appropriate view for a model based on its type.
     * 
     * @param parent The parent model
     * @param model The model to create a view for
     * @param appName The application name
     * @return A view appropriate for the model type
     */
    public static View createViewForModel(ParentModel parent, Model model, String appName) {
        if (model instanceof ParentModel) {
            return new FrameworkParentTrayView((ParentModel)model);
        } else if (model instanceof CheckboxModel) {
            return new FrameworkCheckboxTrayView(parent, model, null, appName);
        } else if (model instanceof ActionModel) {
            return new FrameworkActionTrayView(parent, model, null, appName);
        } else {
            return new FrameworkTrayView(parent, model, null, appName);
        }
    }
}