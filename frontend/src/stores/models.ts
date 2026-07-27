import { defineStore } from 'pinia';

import {
  getAvailableModels,
  getCurrentModelPreference,
  updateModelPreference,
} from '@/api/model-api';
import type { AiModel, AiModelPreference } from '@/types/interview';

export const useModelStore = defineStore('models', {
  state: () => ({
    models: [] as AiModel[],
    preference: null as AiModelPreference | null,
    isLoading: false,
    isSaving: false,
  }),
  getters: {
    selectedModel(state): AiModel | undefined {
      return (
        state.models.find((model) => model.id === state.preference?.modelId) ??
        state.models.find((model) => model.selected)
      );
    },
  },
  actions: {
    async load(force = false) {
      if (this.isLoading || (!force && this.models.length > 0 && this.preference)) {
        return;
      }

      this.isLoading = true;
      try {
        const [models, preference] = await Promise.all([
          getAvailableModels(),
          getCurrentModelPreference(),
        ]);
        this.models = models;
        this.preference = preference;
      } finally {
        this.isLoading = false;
      }
    },
    async select(modelId: number) {
      if (this.preference?.modelId === modelId) {
        return;
      }

      this.isSaving = true;
      try {
        const preference = await updateModelPreference(modelId);
        this.preference = preference;
        this.models = this.models.map((model) => ({
          ...model,
          selected: model.id === preference.modelId,
        }));
      } finally {
        this.isSaving = false;
      }
    },
  },
});
