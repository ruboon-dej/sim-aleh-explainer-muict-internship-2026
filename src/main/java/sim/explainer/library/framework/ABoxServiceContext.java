package sim.explainer.library.framework;

import org.apache.commons.lang3.StringUtils;
import org.semanticweb.owlapi.model.OWLClassAssertionAxiom;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLObjectPropertyAssertionAxiom;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import sim.explainer.library.exception.ErrorCode;
import sim.explainer.library.exception.JSimPiException;
import sim.explainer.library.util.ParserUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Holds the A-Box: the set of individuals Delta, direct concept assertions C(a),
 * and direct role assertions R(a,b). This is the piece that does not exist in the
 * ALEH/FL0/ELH repos - it backs Def. 3.2 (MSC), Def. 4.1's PE(C) extension lookup,
 * and the |Delta| term used throughout the dissimilarity measure.
 *
 * Extensions are computed from asserted facts only (no closure/completion over the
 * T-Box), matching the "canonical interpretation" the paper works against for a
 * given A-Box - this is the same practical approximation the ALEH/FL0/ELH repos use
 * for instance checking rather than running a full tableau reasoner.
 */
@Component
public class ABoxServiceContext {

    private static final Logger logger = LoggerFactory.getLogger(ABoxServiceContext.class);

    public static final String BOTTOM_SENTINEL = "BOTTOM";
    public static final String NEGATION_PREFIX = "NOT_";

    private File aboxFile;

    private final Set<String> individuals = new HashSet<>();
    // individual -> set of primitive concept names directly asserted on it, e.g. {Male, Person}
    private final Map<String, Set<String>> conceptAssertions = new HashMap<>();
    // individual -> role name -> set of successor individuals, i.e. R(a,b)
    private final Map<String, Map<String, Set<String>>> roleAssertions = new HashMap<>();

    private static final Pattern PATTERN_INSTANCE = Pattern.compile("^\\(instance\\s+([A-Za-z0-9_']+)\\s+([A-Za-z0-9_']+)\\)$");
    private static final Pattern PATTERN_RELATED = Pattern.compile("^\\(related\\s+([A-Za-z0-9_']+)\\s+([A-Za-z0-9_']+)\\s+([A-Za-z0-9_']+)\\)$");

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Private /////////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private void addConceptAssertion(String individual, String conceptName) {
        individuals.add(individual);
        conceptAssertions.computeIfAbsent(individual, k -> new HashSet<>()).add(conceptName);
    }

    private void addRoleAssertion(String subject, String role, String object) {
        individuals.add(subject);
        individuals.add(object);
        roleAssertions.computeIfAbsent(subject, k -> new HashMap<>())
                .computeIfAbsent(role, k -> new HashSet<>())
                .add(object);
    }

    private void resetAll() {
        individuals.clear();
        conceptAssertions.clear();
        roleAssertions.clear();
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Public //////////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Reads A-Box assertions out of an already-loaded OWL ontology (Manchester/OWL
     * files typically carry T-Box and A-Box together, unlike KRSS).
     */
    public void initFromOWL(OWLServiceContext owlServiceContext) {
        if (owlServiceContext == null || owlServiceContext.getOwlOntology() == null) {
            throw new JSimPiException("Unable to init A-Box from OWL as owlServiceContext or its ontology is null.", ErrorCode.ABoxServiceContext_IllegalArguments);
        }

        resetAll();

        for (OWLNamedIndividual individual : owlServiceContext.getOwlOntology().getIndividualsInSignature()) {
            String individualName = individual.getIRI().getFragment();
            individuals.add(individualName);

            for (OWLClassAssertionAxiom axiom : owlServiceContext.getOwlOntology().getClassAssertionAxioms(individual)) {
                OWLClassExpression classExpression = axiom.getClassExpression();
                if (!classExpression.isAnonymous()) {
                    addConceptAssertion(individualName, classExpression.asOWLClass().getIRI().getFragment());
                }
            }

            for (OWLObjectPropertyAssertionAxiom axiom : owlServiceContext.getOwlOntology().getObjectPropertyAssertionAxioms(individual)) {
                if (!axiom.getProperty().isAnonymous() && axiom.getObject() instanceof OWLNamedIndividual) {
                    String roleName = axiom.getProperty().asOWLObjectProperty().getIRI().getFragment();
                    String objectName = ((OWLNamedIndividual) axiom.getObject()).getIRI().getFragment();
                    addRoleAssertion(individualName, roleName, objectName);
                }
            }
        }

        if (logger.isInfoEnabled()) {
            logger.info("Loaded A-Box from OWL ontology: |Delta|=" + individuals.size());
        }
    }

    /**
     * Reads A-Box assertions from a KRSS-style file, one assertion per matched line:
     * {@code (instance a C)} for a concept assertion C(a), and
     * {@code (related a b R)} for a role assertion R(a,b).
     */
    public void initFromKRSSFile(String aboxFilePath) {
        if (aboxFilePath == null) {
            throw new JSimPiException("Unable to init A-Box as aboxFilePath is null.", ErrorCode.ABoxServiceContext_IllegalArguments);
        }

        resetAll();
        this.aboxFile = new File(aboxFilePath);

        try (BufferedReader reader = new BufferedReader(new FileReader(aboxFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String trimmed = ParserUtils.compactConceptDescriptionString(StringUtils.trim(line));
                if (trimmed.isEmpty()) {
                    continue;
                }

                Matcher instanceMatcher = PATTERN_INSTANCE.matcher(trimmed);
                if (instanceMatcher.matches()) {
                    addConceptAssertion(instanceMatcher.group(1), instanceMatcher.group(2));
                    continue;
                }

                Matcher relatedMatcher = PATTERN_RELATED.matcher(trimmed);
                if (relatedMatcher.matches()) {
                    addRoleAssertion(relatedMatcher.group(1), relatedMatcher.group(3), relatedMatcher.group(2));
                }
            }
        }

        catch (FileNotFoundException e) {
            resetAll();
            throw new JSimPiException("Unable to read A-Box file from path[" + aboxFilePath + "] due to file not found exception.", e, ErrorCode.ABoxServiceContext_FileNotFoundException);
        }

        catch (IOException e) {
            resetAll();
            throw new JSimPiException("Unable to read A-Box file from path[" + aboxFilePath + "] due to io exception.", e, ErrorCode.ABoxServiceContext_IOException);
        }

        if (logger.isInfoEnabled()) {
            logger.info("Loaded A-Box from KRSS file[" + aboxFilePath + "]: |Delta|=" + individuals.size());
        }
    }

    /**
     * PE(P): the extension of a single (possibly negated) primitive concept name,
     * i.e. {@literal {x in Delta | P holds of x}}. Negation (NOT_ prefix, as produced
     * by the KRSS/Manchester concept-set handlers) is interpreted as complement
     * within Delta. The BOTTOM sentinel (produced when parsing detects a direct
     * P / NOT_P contradiction) always has empty extension.
     */
    public Set<String> getPrimitiveConceptExtension(String primitiveConceptName) {
        if (primitiveConceptName == null) {
            throw new JSimPiException("Unable to get extension as primitiveConceptName is null.", ErrorCode.ABoxServiceContext_IllegalArguments);
        }

        if (primitiveConceptName.equals(BOTTOM_SENTINEL)) {
            return Collections.emptySet();
        }

        boolean negated = primitiveConceptName.startsWith(NEGATION_PREFIX);
        String baseName = negated ? primitiveConceptName.substring(NEGATION_PREFIX.length()) : primitiveConceptName;

        Set<String> asserted = new HashSet<>();
        for (Map.Entry<String, Set<String>> entry : conceptAssertions.entrySet()) {
            if (entry.getValue().contains(baseName)) {
                asserted.add(entry.getKey());
            }
        }

        if (!negated) {
            return asserted;
        }

        Set<String> complement = new HashSet<>(individuals);
        complement.removeAll(asserted);
        return complement;
    }

    /**
     * PE(prim(C)): the extension of a conjunction of (possibly negated) primitive
     * concepts, i.e. the intersection of each individual primitive's extension.
     * An empty conjunction (prim(C) is empty, as with the top concept) is
     * interpreted as matching everyone, i.e. Delta itself.
     */
    public Set<String> getConjunctionExtension(Set<String> primitiveConceptNames) {
        if (primitiveConceptNames == null) {
            throw new JSimPiException("Unable to get conjunction extension as primitiveConceptNames is null.", ErrorCode.ABoxServiceContext_IllegalArguments);
        }

        if (primitiveConceptNames.isEmpty()) {
            return new HashSet<>(individuals);
        }

        Set<String> result = null;
        for (String primitiveConceptName : primitiveConceptNames) {
            Set<String> extension = getPrimitiveConceptExtension(primitiveConceptName);
            if (result == null) {
                result = new HashSet<>(extension);
            } else {
                result.retainAll(extension);
            }
        }

        return result;
    }

    public Set<String> getRoleSuccessors(String individual, String roleName) {
        if (individual == null || roleName == null) {
            throw new JSimPiException("Unable to get role successors as individual or roleName is null.", ErrorCode.ABoxServiceContext_IllegalArguments);
        }

        Map<String, Set<String>> rolesForIndividual = roleAssertions.get(individual);
        if (rolesForIndividual == null || !rolesForIndividual.containsKey(roleName)) {
            return Collections.emptySet();
        }

        return rolesForIndividual.get(roleName);
    }

    public Set<String> getAssertedConcepts(String individual) {
        if (individual == null) {
            throw new JSimPiException("Unable to get asserted concepts as individual is null.", ErrorCode.ABoxServiceContext_IllegalArguments);
        }

        if (!individuals.contains(individual)) {
            throw new JSimPiException("Unable to get asserted concepts as individual[" + individual + "] is unknown to the A-Box.", ErrorCode.ABoxServiceContext_UnknownIndividualException);
        }

        return conceptAssertions.getOrDefault(individual, Collections.emptySet());
    }

    public Map<String, Set<String>> getAssertedRoles(String individual) {
        if (individual == null) {
            throw new JSimPiException("Unable to get asserted roles as individual is null.", ErrorCode.ABoxServiceContext_IllegalArguments);
        }

        if (!individuals.contains(individual)) {
            throw new JSimPiException("Unable to get asserted roles as individual[" + individual + "] is unknown to the A-Box.", ErrorCode.ABoxServiceContext_UnknownIndividualException);
        }

        return roleAssertions.getOrDefault(individual, Collections.emptyMap());
    }

    public boolean containsIndividual(String individual) {
        return individual != null && individuals.contains(individual);
    }

    public int getDomainSize() {
        return individuals.size();
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Getters /////////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public File getAboxFile() {
        return aboxFile;
    }

    public Set<String> getIndividuals() {
        return individuals;
    }
}
