package com.research.assistant;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

@Service
public class ResearchService {

    @Value("${gemini.api.url}")
    private  String geminiApiUrl;
    @Value("${gemini.api.key}")
    private String geminiApiKey;

    private final WebClient webClient;

    private final ObjectMapper objectMapper;

    public ResearchService(WebClient.Builder webClientBuilder,ObjectMapper objectMapper) {
        this.webClient = webClientBuilder.build();
        this.objectMapper=objectMapper;
    }

    public String processContent(ResearchRequest request) {

        //Build the prompt
        String prompt=buildPrompt(request);

        //Query AI model API
        Map<String,Object>  requestBody= Map.of(
                "contents",new Object[]{
                        Map.of("parts",new Object[]{
                                Map.of("text",prompt)
                        })
                }
        );
        // Parse the respone

        String response=webClient.post()
                .uri(geminiApiUrl+geminiApiKey)
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(String.class)
                .block();
        // return response
        return extractTextFromResponse(response);
    }

    private String extractTextFromResponse(String response) {
        try{
            GeminiResponse geminiResponse=objectMapper.readValue(response, GeminiResponse.class);
            if(geminiResponse.getCandidates()!=null && !geminiResponse.getCandidates().isEmpty()){
                GeminiResponse.Candidate firstCandidate=geminiResponse.getCandidates().get(0);
                if(firstCandidate.getContent()!=null && firstCandidate.getContent().getParts()!=null && !firstCandidate.getContent().getParts().isEmpty()){
                    return firstCandidate.getContent().getParts().get(0).getText();
                }
            }
            return "No content found in response";
        }catch (Exception e){
            return "Error parsing : "+e.getMessage();
        }
    }

    public String buildPrompt(ResearchRequest request){

        StringBuilder prompt= new StringBuilder();
        switch (request.getOperation()){

            case "summarize":
                prompt.append("provide a clear and concize summary of the following text in a few sentence:\n\n");
                break;
            case "suggest":
                prompt.append("based one the following content :suggest related topics and futher reading .format the response with clear heading and bullet points:\n\n");
                break;
            default:
                throw new IllegalArgumentException("Unknown operation: "+ request.getOperation());
        }
        prompt.append(request.getContent());
         return prompt.toString();
    }
}
