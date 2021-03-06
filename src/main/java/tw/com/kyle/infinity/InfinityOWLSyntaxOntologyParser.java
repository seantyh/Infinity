package tw.com.kyle.infinity;

/* This file is part of the OWL API.
 * The contents of this file are subject to the LGPL License, Version 3.0.
 * Copyright 2014, The University of Manchester
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.  If not, see http://www.gnu.org/licenses/.
 *
 * Alternatively, the contents of this file may be used under the terms of the Apache License, Version 2.0 in which case, the provisions of the Apache License Version 2.0 are applicable instead of those above.
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License. */

import org.semanticweb.owlapi.expression.OWLEntityChecker;
import org.semanticweb.owlapi.expression.ShortFormEntityChecker;
import org.semanticweb.owlapi.formats.ManchesterSyntaxDocumentFormatFactory;
import org.semanticweb.owlapi.io.AbstractOWLParser;
import org.semanticweb.owlapi.io.DocumentSources;
import org.semanticweb.owlapi.io.OWLOntologyDocumentSource;
import org.semanticweb.owlapi.io.OWLOntologyInputSourceException;
import org.semanticweb.owlapi.manchestersyntax.parser.ManchesterOWLSyntax;
import org.semanticweb.owlapi.manchestersyntax.parser.ManchesterOWLSyntaxParserException;
import org.semanticweb.owlapi.manchestersyntax.parser.ManchesterOWLSyntaxParserImpl;
import org.semanticweb.owlapi.manchestersyntax.renderer.ParserException;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.util.BidirectionalShortFormProvider;
import org.semanticweb.owlapi.util.ShortFormProvider;
import org.semanticweb.owlapi.util.SimpleShortFormProvider;
import org.semanticweb.owlapi.util.mansyntax.ManchesterOWLSyntaxParser;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;

/**
 * @author Matthew Horridge, The University Of Manchester, Bio-Health Informatics Group
 * @since 2.1.1
 */
public class InfinityOWLSyntaxOntologyParser extends AbstractOWLParser {

    private static final String COMMENT_START_CHAR = "#";

    private static boolean startsWithMagicNumber(String line) {
        return line.indexOf(ManchesterOWLSyntax.PREFIX.toString()) != -1
                || line.indexOf(ManchesterOWLSyntax.ONTOLOGY
                .toString()) != -1;
    }

    @Override
    public OWLDocumentFormat parse(OWLOntologyDocumentSource documentSource,
                                   OWLOntology ontology, OWLOntologyLoaderConfiguration configuration) {
        return parse(documentSource, ontology, null, null, configuration);
    }

    @Override
    public OWLDocumentFormatFactory getSupportedFormat() {
        return new ManchesterSyntaxDocumentFormatFactory();
    }

    public OWLDocumentFormat parse(OWLOntologyDocumentSource source, OWLOntology ontology, OWLOntology default_onto,
                                   BidirectionalShortFormProvider shortFormProvider, OWLOntologyLoaderConfiguration config) {
        try (Reader r = DocumentSources.wrapInputAsReader(source, config);
             BufferedReader reader = new BufferedReader(r)) {
            StringBuilder sb = new StringBuilder();
            String line;
            int lineCount = 1;
            // Try to find the "magic number" (Prefix: or Ontology:)
            boolean foundMagicNumber = false;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
                sb.append('\n');
                if (!foundMagicNumber) {
                    String trimmedLine = line.trim();
                    if (!trimmedLine.isEmpty() && !trimmedLine.startsWith(COMMENT_START_CHAR)) {
                        // Non-empty line, that is not a comment. The
                        // trimmed line MUST start with our magic
                        // number if we are going to parse the rest of
                        // it.
                        if (startsWithMagicNumber(line)) {
                            foundMagicNumber = true;
                            // We have set the found flag - we never end
                            // up here again
                        } else {
                            // Non-empty line that is NOT a comment. We
                            // cannot possibly parse this.
                            int startCol = line.indexOf(trimmedLine) + 1;
                            String msg = String.format(
                                    "Encountered '%s' at line %s column %s.  Expected either 'Ontology:' or 'Prefix:'",
                                    trimmedLine, Integer.valueOf(lineCount), Integer.valueOf(startCol));
                            throw new ManchesterOWLSyntaxParserException(msg, lineCount, startCol);
                        }
                    }
                }
                lineCount++;
            }
            String s = sb.toString();
            ManchesterOWLSyntaxParser parser = new ManchesterOWLSyntaxParserImpl(
                    new OntologyConfigurator(), ontology
                    .getOWLOntologyManager().getOWLDataFactory());
            parser.setOntologyLoaderConfiguration(config);
            parser.setStringToParse(s);
            if (default_onto != null){
                parser.setDefaultOntology(default_onto);
            }

            parser.setDefaultOntology(ontology);
            if (shortFormProvider != null) {
                OWLEntityChecker entityChecker = new ShortFormEntityChecker(shortFormProvider);
                parser.setOWLEntityChecker(entityChecker);
            }


            return parser.parseOntology(ontology);
        } catch (ParserException e) {
            throw new ManchesterOWLSyntaxParserException(e.getMessage(), e, e.getLineNumber(),
                    e.getColumnNumber());
        } catch (OWLOntologyInputSourceException | IOException e) {
            throw new ManchesterOWLSyntaxParserException(e.getMessage(), e, 1, 1);
        }
    }
}
