package sim.explainer.library.framework.unfolding;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

import sim.explainer.library.enumeration.KRSSConstant;
import sim.explainer.library.exception.ErrorCode;
import sim.explainer.library.exception.JSimPiException;
import sim.explainer.library.framework.KRSSServiceContext;

@Component("subRoleUnfolderKRSSSyntax")
public class SubRoleUnfolderKRSSSyntax implements ISubRoleUnfolder {

    private final KRSSServiceContext krssServiceContext;

    private Map<String, String> fullRoleDefinitionMap;
    private Map<String, String> primitiveRoleDefinitionMap;


    public SubRoleUnfolderKRSSSyntax(KRSSServiceContext krssServiceContext) {
        this.krssServiceContext = krssServiceContext;

    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Private /////////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private Set<String> unfold(String role, Set<String> subRoles) {
        subRoles.add(role);

        // Replace the existing loop in unfold() inside SubRoleUnfolderKRSSSyntax.java
        for (Map.Entry<String, String> entry : fullRoleDefinitionMap.entrySet()) {
            String candidateRole = entry.getKey();
            String candidateDefinition = entry.getValue();

            // Use \\b to ensure word boundaries (so 'base' does not match 'hasBase')
            // We escape the role name to ensure no regex special characters in the role name cause errors
            String regex = "\\b" + Pattern.quote(role) + "\\b";
            
            if (candidateDefinition.matches(".*" + regex + ".*") && !subRoles.contains(candidateRole)) {
                unfold(candidateRole, subRoles);
            }
        }

        // Do the exact same for the primitiveRoleDefinitionMap loop below it
        for (Map.Entry<String, String> entry : primitiveRoleDefinitionMap.entrySet()) {
            String candidateRole = entry.getKey();
            String candidateDefinition = entry.getValue();

            String regex = "\\b" + Pattern.quote(role) + "\\b";
            
            if (candidateDefinition.matches(".*" + regex + ".*") && !subRoles.contains(candidateRole)) {
                unfold(candidateRole, subRoles);
            }
        }

        return subRoles;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Public //////////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public Set<String> unfoldSubRoleHierarchy(String roleName) {
        if (roleName == null) {
            throw new JSimPiException("Unable to unfold sub role hierarchy due to roleName is null.",
                    ErrorCode.SuperRoleUnfolderKRSSSyntax_IllegalArguments);
        }

        this.fullRoleDefinitionMap = krssServiceContext.getFullRoleDefinitionMap();
        this.primitiveRoleDefinitionMap = krssServiceContext.getPrimitiveRoleDefinitionMap();

        Set<String> roles = new HashSet<>();
        if (roleName.equals(KRSSConstant.TOP_ROLE.getStr())) {
            return roles;
        }

        if (roleName.equals(KRSSConstant.BOTTOM_ROLE.getStr())) {
            return roles;
        }

        return unfold(roleName, roles);
    }
}