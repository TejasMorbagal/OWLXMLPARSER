import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.formats.OWLXMLDocumentFormat;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.rdf.rdfxml.renderer.OWLOntologyXMLNamespaceManager;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.reasoner.OWLReasonerFactory;
import org.semanticweb.owlapi.reasoner.SimpleConfiguration;
import org.semanticweb.owlapi.reasoner.structural.StructuralReasonerFactory;
import org.semanticweb.owlapi.search.EntitySearcher;


import java.io.File;
import java.lang.annotation.Annotation;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Arrays.stream;
import static org.semanticweb.owlapi.model.AxiomType.CLASS_ASSERTION;

//https://github.com/owlcs/owlapi/wiki/Migrate-from-version-3.4-and-3.5-to-4.0
//https://github.com/phillord/owl-api/blob/master/contract/src/test/java/org/coode/owlapi/examples/Examples.java
//https://sourceforge.net/p/owlapi/mailman/
//https://www.javatips.net/api/org.semanticweb.owlapi.model.owlclassaxiom
//https://github.com/phillord/hermit-reasoner/blob/master/examples/org/semanticweb/HermiT/examples/MaterialiseInferences.java
//https://github.com/OntoZoo/ontobull/blob/master/src/main/java/org/hegroup/bfoconvert/service/OWLFileManager.java
//https://tutorial-academy.com/owlapi-5-read-class-restriction-axiom-visitor/
public class OWLAPIFirst {
    private static Object OWLQuantifiedObjectRestriction;

    @SuppressWarnings("deprecation")
	public static void main(String[] args) {

        //object to manage the ontology
        OWLOntologyManager man = OWLManager.createOWLOntologyManager();

        String filepath = "/home/hduser/Downloads/univ-bench.owl";
        String sep = System.getProperty("file.separator");
        String filename = filepath.substring(filepath.lastIndexOf(sep) + 1, filepath.length());
        System.out.println("Input : " + filename + " (" + filepath + ")\n");
        File file = new File(filepath);

        OWLOntology ontology = null;
        try {
            //load an ontology from file
            ontology = man.loadOntologyFromOntologyDocument(file);
        } catch (OWLOntologyCreationException e) {
            System.out.println("[Invalid IRI]\tUnable to load ontology.\n\tInput: " + filepath +"\n");
        }

        OWLDocumentFormat format = null;
        if (ontology != null) {
            // get the ontology format
            format = man.getOntologyFormat(ontology);
        }
        OWLXMLDocumentFormat owlxmlFormat = new OWLXMLDocumentFormat();
        if (format != null && format.isPrefixOWLDocumentFormat()) {
            // copy all the prefixes from OWL document format to OWL XML format
            owlxmlFormat.copyPrefixesFrom(format.asPrefixOWLDocumentFormat());
        }

        try {
            if (ontology != null) {
                // create an ontology IRI out of a physical URI
                // save the ontology in OWL XML format
                man.saveOntology(ontology, owlxmlFormat, IRI.create(file.toURI()));
            }
        } catch (OWLOntologyStorageException e) {
            System.out.println("Unable to save ontology in XML format.");
        }

        if(ontology != null) {

            /*// get a reference to a data factory from an OWLOntologyManager.
            OWLDataFactory factory = man.getOWLDataFactory();

            OWLReasonerFactory rf = new ReasonerFactory();
            OWLReasoner r = rf.createReasoner(ontology);
            r.precomputeInferences(InferenceType.CLASS_HIERARCHY);*/

            OWLDataFactory factory = man.getOWLDataFactory();
            Stream<OWLAxiom> axioms = null;
            axioms = ontology.axioms();


            if (axioms != null){
                System.out.println("\nLoaded ontology with " + ontology.getAxiomCount() + " axioms");
            }
            else{
                System.out.println("\nLoaded ontology contains zero axioms.");
            }

            /*
            prints the namespaces that were used in the file an ontology was loaded from.
             */
            OWLOntologyXMLNamespaceManager nsManager = new OWLOntologyXMLNamespaceManager(ontology, owlxmlFormat);
            System.out.println("\n\nNamespaces that were used in the file an ontology was loaded from:");
            for (String prefix : nsManager.getPrefixes()) {
                if (prefix.length() != 0) {
                    System.out.println(prefix + " --> " + nsManager.getNamespaceForPrefix(prefix));
                }
                else {
                    System.out.println("Default: " + nsManager.getDefaultNamespace());
                }
            }

            /* to read and display the objectproperty axiom */
          
            int classesInOntologyCount = ontology.classesInSignature().collect(Collectors.toSet()).size();
            if (classesInOntologyCount>0){
                System.out.println("\n\nNumber of classes in the loaded ontology: "+classesInOntologyCount+"\n");
                // get all classes in ontology signature
                for (OWLClass owlClass : ontology.classesInSignature().collect(Collectors.toSet())) {
                    OWLDeclarationAxiom declaration = factory.getOWLDeclarationAxiom(owlClass);
                    System.out.println(declaration);
                    Stream<OWLAnnotation> annotation = EntitySearcher.getAnnotations(owlClass.getIRI(), ontology);
                    annotation.forEach(System.out::println);

                    // get all axioms for each class
                    for (OWLAxiom owlClassAxiom : ontology.axioms(owlClass).collect(Collectors.toSet())) {
                        // create an object visitor to get to the subClass restrictions

                        /*Stream<OWLSubClassOfAxiom> allSubClassAxioms = ontology.subClassAxiomsForSuperClass(owlClass);
                        System.out.println("Printing SubClasses");
                        allSubClassAxioms.forEach(System.out::println);*/


                        owlClassAxiom.accept(new OWLObjectVisitor() {

                            // found the subClassOf axiom
                            public void visit(OWLSubClassOfAxiom subClassAxiom) {

                                if (subClassAxiom.getSuperClass() instanceof OWLClass && subClassAxiom.getSubClass() instanceof OWLClass) {
                                    System.out.println(subClassAxiom.toString());
                                }
                                // create an object visitor to read the underlying (subClassOf) restrictions
                                subClassAxiom.getSuperClass().accept(new OWLObjectVisitor() {

                                    public void visit(OWLObjectSomeValuesFrom someValuesFromAxiom) {
                                        System.out.println( subClassAxiom.toString() );
                                        System.out.println( someValuesFromAxiom.getClassExpressionType().toString() );
                                        System.out.println( someValuesFromAxiom.getProperty().toString() );
                                        System.out.println( someValuesFromAxiom.getFiller().toString() );
                                    }

                                    public void visit(OWLObjectExactCardinality exactCardinalityAxiom) {
                                        System.out.println(subClassAxiom.toString() );
                                        System.out.println(exactCardinalityAxiom.getClassExpressionType().toString() );
                                        System.out.println(exactCardinalityAxiom.getCardinality() );
                                        System.out.println( exactCardinalityAxiom.getProperty().toString() );
                                        System.out.println(exactCardinalityAxiom.getFiller().toString() );
                                    }

                                    public void visit(OWLObjectMinCardinality minCardinalityAxiom) {
                                        System.out.println( subClassAxiom.toString() );
                                        System.out.println(  minCardinalityAxiom.getClassExpressionType().toString() );
                                        System.out.println(  minCardinalityAxiom.getCardinality() );
                                        System.out.println(  minCardinalityAxiom.getProperty().toString() );
                                        System.out.println(  minCardinalityAxiom.getFiller().toString() );
                                    }

                                    public void visit(OWLObjectMaxCardinality maxCardinalityAxiom) {
                                        System.out.println(  subClassAxiom.toString() );
                                        System.out.println(  maxCardinalityAxiom.getClassExpressionType().toString() );
                                        System.out.println(  maxCardinalityAxiom.getCardinality() );
                                        System.out.println(  maxCardinalityAxiom.getProperty().toString() );
                                        System.out.println(  maxCardinalityAxiom.getFiller().toString() );
                                    }

                                });
                            }
                        });

                    }

                    System.out.println();
                }
            } else {
                System.out.println("There are no classes in the loaded ontology\n");
            }
            
            // System.out.println("Tejas");
            //Stream<OWLNamedIndividual> inds= ontology.individualsInSignature();
           // Set<OWLNamedIndividual> index = inds.collect(Collectors.toSet());
           // for (OWLNamedIndividual ind: index){
            //    System.out.println(ind.getDataPropertiesInSignature());
            //}
           // Set<OWLObjectProperty> prop;
           // Set<OWLDataProperty> dataProp;
           // Set<OWLNamedIndividual> individuals;

           // prop = ontology.getObjectPropertiesInSignature();
            //dataProp = ontology.getDataPropertiesInSignature();
           // individuals = ontology.getIndividualsInSignature();
            for (OWLObjectPropertyDomainAxiom op : ontology.getAxioms(AxiomType.OBJECT_PROPERTY_DOMAIN))
            {
            //	OWLDeclarationAxiom declaration = factory.getOWLDeclarationAxiom(op);
               // System.out.println(declaration);
            	
            	System.out.println(op.toString());
            }


            /*for (final OWLSubClassOfAxiom subClasse : ontology.getAxioms(AxiomType.SUBCLASS_OF)) {
                if (subClasse.getSuperClass() instanceof OWLClass && subClasse.getSubClass() instanceof OWLClass) {
                    System.out.println(subClasse.getSubClass() + " extends " + subClasse.getSuperClass());
                }
            }*/

            /*//System.out.println(axioms.classesInSignature());
            Iterator<OWLAxiom> listOfAxioms = ontology.axioms().iterator();
            while (listOfAxioms.hasNext()) {
                OWLAxiom axiom = listOfAxioms.next();
                System.out.println(axiom.getAxiomType());
                System.out.println("Axiom : " + axiom);
                if ((axiom instanceof OWLClassAxiom))
                    System.out.println("is Axiom a OWL Class ");
                if (!axiom.isLogicalAxiom()) {
                    *//*if(axiom instanceof OWLAnnotationAssertionAxiom ){
                        System.out.println("processing an AnnotationAxiom");
                        OWLAnnotationAssertionAxiom annotationAxiom = (OWLAnnotationAssertionAxiom) axiom;
                        OWLAnnotationSubject subject = annotationAxiom.getSubject();
                        System.out.println(subject);
                    }*//*
                    if (axiom instanceof OWLDeclarationAxiom) {
                        System.out.println("processing an OWLDeclarationAxiom");
                        OWLDeclarationAxiom declarationAxiom = (OWLDeclarationAxiom) axiom;
                        OWLEntity entity = declarationAxiom.getEntity();
                        if (entity.isType(EntityType.CLASS)) {
                            System.out.println("OWL CLASS");
                            OWLClass cls = entity.asOWLClass();
                            r.getSubClasses(cls).forEach(System.out::println);
//                            OWLDeclarationAxiom da = factory.getOWLDeclarationAxiom(cls);
//                            System.out.println(da);

                        }
                        *//*EntityType entityType = entity.getEntityType();
                        System.out.println(entityType);
                        System.out.println(entityType.isOWLClass());*//*
                    }

                }
                for (final OWLSubClassOfAxiom subClasse : ontology.getAxioms(AxiomType.SUBCLASS_OF)) {
                    if (subClasse.getSuperClass() instanceof OWLClass && subClasse.getSubClass() instanceof OWLClass) {
                        System.out.println(subClasse.getSubClass() + " extends " + subClasse.getSuperClass());
                    }
                }
            }*/






        }



            /*//IRI documentIRI = man.getOntologyDocumentIRI(ontology);
            OWLDataFactory df = ontology.getOWLOntologyManager().getOWLDataFactory();
            OWLDocumentFormat format = man.getOntologyFormat(ontology);
            OWLXMLDocumentFormat owlxmlFormat = new OWLXMLDocumentFormat();
            if (format != null && format.isPrefixOWLDocumentFormat()) {
                owlxmlFormat.copyPrefixesFrom(format.asPrefixOWLDocumentFormat());
            }
            man.saveOntology(ontology, owlxmlFormat, IRI.create(file.toURI()));

            *//*
            prints the namespaces that were used in the file an ontology was loaded from.
             *//*
            OWLOntologyXMLNamespaceManager nsManager = new OWLOntologyXMLNamespaceManager(ontology, owlxmlFormat);
            System.out.println("\n\nNamespaces that were used in the file an ontology was loaded from:");
            for (String prefix : nsManager.getPrefixes()) {
                if (prefix.length() != 0) {
                    System.out.println(prefix + " --> " + nsManager.getNamespaceForPrefix(prefix));
                }
                else {
                    System.out.println("Default: " + nsManager.getDefaultNamespace());
                }
            }

            System.out.println(ontology);
            System.out.println("Number of axioms: " + ontology.getAxiomCount());
            //System.out.println(" from: " + documentIRI);
            System.out.println(" format: " + owlxmlFormat);

            System.out.println("  " + ontology.getLogicalAxiomCount() + " logical axioms");
            ontology.logicalAxioms().forEach(System.out::println);
            //ontology.importsClosure().forEach(System.out::println);




        } catch (OWLOntologyStorageException e) {
            e.printStackTrace();
        }*/
    }


}




            

